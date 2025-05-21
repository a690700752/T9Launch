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

    public void fetchAndParseBookmarks(boolean forceRefresh, BookmarksCallback callback) {
        if (!forceRefresh) {
            List<Bookmark> cachedBookmarks = loadBookmarksFromCache();
            if (cachedBookmarks != null && !cachedBookmarks.isEmpty()) {
                // console.warn("从缓存加载书签");
                callback.onSuccess(cachedBookmarks);
                return;
            }
        }

        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREFS_SETTINGS_NAME, Context.MODE_PRIVATE);
        String webdavUrl = prefs.getString(SettingsActivity.KEY_WEBDAV_URL, null);
        String username = prefs.getString(SettingsActivity.KEY_WEBDAV_USERNAME, null);
        String password = prefs.getString(SettingsActivity.KEY_WEBDAV_PASSWORD, null);
        String bookmarkFilename = prefs.getString(SettingsActivity.KEY_BOOKMARK_FILENAME, null);

        if (webdavUrl == null || bookmarkFilename == null) {
            callback.onFailure(new IOException("WebDAV URL 或文件名未配置"));
            return;
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

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // console.warn("WebDAV 请求失败 (OkHttp onFailure): " + e.getMessage());
                if (!forceRefresh) {
                    List<Bookmark> cachedBookmarks = loadBookmarksFromCache();
                    if (cachedBookmarks != null && !cachedBookmarks.isEmpty()) {
                        callback.onSuccess(cachedBookmarks);
                        return;
                    }
                }
                callback.onFailure(e);
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
                        bookmarksResult = parser.parse(inputStream, fullUrl);
                        saveBookmarksToCache(bookmarksResult);
                        // console.warn("从网络加载书签并缓存: " + bookmarkFilename);
                        callback.onSuccess(bookmarksResult);
                    } catch (Exception parsingException) { // 捕获解析或流处理中的异常
                        // console.warn("解析书签时出错: " + parsingException.getMessage());
                        if (!forceRefresh) {
                            List<Bookmark> cachedBookmarks = loadBookmarksFromCache();
                            if (cachedBookmarks != null && !cachedBookmarks.isEmpty()) {
                                callback.onSuccess(cachedBookmarks);
                                return;
                            }
                        }
                        callback.onFailure(parsingException);
                    }
                } catch (IOException networkOrSetupException) { // 捕获响应处理本身的异常
                    // console.warn("网络或响应处理错误: " + networkOrSetupException.getMessage());
                     if (!forceRefresh) {
                        List<Bookmark> cachedBookmarks = loadBookmarksFromCache();
                        if (cachedBookmarks != null && !cachedBookmarks.isEmpty()) {
                            callback.onSuccess(cachedBookmarks);
                            return;
                        }
                    }
                    callback.onFailure(networkOrSetupException);
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
