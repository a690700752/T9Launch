package com.moonveil.t9launch;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.moonveil.t9launch.parsers.BookmarkParser;
import com.moonveil.t9launch.parsers.HtmlBookmarkParser;
import com.moonveil.t9launch.parsers.XbelBookmarkParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WebdavService {

    public interface BookmarksCallback {
        void onSuccess(List<Bookmark> bookmarks);
        void onFailure(Exception e);
    }

    private Context context;
    private OkHttpClient httpClient;
    private static final String BOOKMARKS_CACHE_FILE = "bookmarks_cache.txt";

    private BookmarkParser htmlParser;
    private BookmarkParser xbelParser;

    public WebdavService(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        this.htmlParser = new HtmlBookmarkParser();
        this.xbelParser = new XbelBookmarkParser();
    }

    public void fetchAndParseBookmarks(BookmarksCallback callback) {
        List<Bookmark> cachedBookmarks = loadBookmarksFromCache();
        boolean initialCallbackDone = false;

        if (cachedBookmarks != null && !cachedBookmarks.isEmpty()) {
            // console.warn("从缓存加载书签 (立即返回)");
            callback.onSuccess(cachedBookmarks);
            initialCallbackDone = true;
            // We don't return here; proceed to update cache in the background.
        }

        // Continue to fetch from network to update cache
        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREFS_SETTINGS_NAME, Context.MODE_PRIVATE);
        String webdavUrl = prefs.getString(SettingsActivity.KEY_WEBDAV_URL, null);
        String username = prefs.getString(SettingsActivity.KEY_WEBDAV_USERNAME, null);
        String password = prefs.getString(SettingsActivity.KEY_WEBDAV_PASSWORD, null);
        String bookmarkFilename = prefs.getString(SettingsActivity.KEY_BOOKMARK_FILENAME, null);

        if (webdavUrl == null || bookmarkFilename == null) {
            if (!initialCallbackDone) { // Only fail if nothing was returned via cache yet
                callback.onFailure(new IOException("WebDAV URL 或文件名未配置"));
            } else {
                // Optional: Log that background update is skipped due to missing config
                // android.util.Log.w("WebdavService", "WebDAV URL or filename not configured, skipping background update.");
            }
            return; // Stop further processing for network request
        }

        if (!webdavUrl.endsWith("/")) {
            webdavUrl += "/";
        }
        String fullUrl = webdavUrl + bookmarkFilename;

        Request.Builder requestBuilder = new Request.Builder().url(fullUrl).get();

        if (username != null && !username.isEmpty() && password != null) {
            String credentials = username + ":" + password;
            String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            requestBuilder.header("Authorization", basicAuth);
        }

        Request request = requestBuilder.build();

        final boolean effectivelyFinalInitialCallbackDone = initialCallbackDone;
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // console.warn("后台更新 - WebDAV 请求失败: " + e.getMessage());
                if (!effectivelyFinalInitialCallbackDone) { // If cache was empty and network failed
                    callback.onFailure(e);
                }
                // Otherwise, the user already has cached data; this failure is for the background update.
            }

            @Override
            public void onResponse(Call call, Response response) {
                List<Bookmark> bookmarksResult;
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("WebDAV 请求不成功，响应码: " + response.code());
                    }

                    ResponseBody body = response.body();
                    if (body == null) {
                        throw new IOException("WebDAV 响应体为空");
                    }

                    try (InputStream inputStream = body.byteStream()) {
                        BookmarkParser parser;
                        if (bookmarkFilename != null && bookmarkFilename.toLowerCase().endsWith(".xbel")) {
                            parser = xbelParser;
                        } else {
                            parser = htmlParser; // 默认为 HTML 解析器
                        }
                        List<Bookmark> networkBookmarks = parser.parse(inputStream, fullUrl);
                        saveBookmarksToCache(networkBookmarks); // Update cache for next time
                        // console.warn("后台更新 - 从网络加载书签并更新缓存: " + bookmarkFilename);

                        if (!effectivelyFinalInitialCallbackDone) { // If cache wasn't returned initially (e.g., it was empty)
                            callback.onSuccess(networkBookmarks);
                        }
                        // If cache WAS returned, we don't call onSuccess again for the background update.
                    } catch (Exception parsingException) { // 捕获解析或流处理中的异常
                        // console.warn("后台更新 - 解析书签时出错: " + parsingException.getMessage());
                        if (!effectivelyFinalInitialCallbackDone) {
                            callback.onFailure(parsingException);
                        }
                        // If initial callback was done, this parsing error is for the background update.
                    }
                } catch (IOException networkOrSetupException) { // 捕获响应处理本身的异常
                    // console.warn("后台更新 - 网络或响应处理错误: " + networkOrSetupException.getMessage());
                    if (!effectivelyFinalInitialCallbackDone) {
                        callback.onFailure(networkOrSetupException);
                    }
                    // If initial callback was done, this error is for the background update.
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
            }
        });
    }

    private void saveBookmarksToCache(List<Bookmark> bookmarks) {
        File cacheFile = new File(context.getCacheDir(), BOOKMARKS_CACHE_FILE);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile))) {
            for (Bookmark bookmark : bookmarks) {
                writer.write(bookmark.getName() + "," + bookmark.getUrl());
                writer.newLine();
            }
        } catch (IOException e) {
            // console.warn("保存书签到缓存失败: " + e.getMessage());
        }
    }

    private List<Bookmark> loadBookmarksFromCache() {
        File cacheFile = new File(context.getCacheDir(), BOOKMARKS_CACHE_FILE);
        if (!cacheFile.exists()) {
            return null;
        }
        List<Bookmark> bookmarks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    bookmarks.add(new Bookmark(parts[0].trim(), parts[1].trim()));
                }
            }
        } catch (IOException e) {
            // console.warn("从缓存加载书签失败: " + e.getMessage());
            return null;
        }
        return bookmarks;
    }
}
