package com.moonveil.t9launch;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.GridLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextInputEditText searchBox;
    private AppListAdapter appListAdapter;
    private List<AppInfo> allApps = new ArrayList<>();
    private List<Bookmark> allBookmarks = new ArrayList<>();
    private AppLRUCache appLRUCache;
    private WebdavService webdavService;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appLRUCache = new AppLRUCache(this);
        webdavService = new WebdavService(this);

        searchBox = findViewById(R.id.searchBox);
        MaterialCardView keypadCard = findViewById(R.id.keypadContainer);
        GridLayout keypadGrid = (GridLayout) keypadCard.getChildAt(0);
        RecyclerView appList = findViewById(R.id.appList);

        // 设置RecyclerView为网格布局
        int spanCount = 4; // 每行显示的应用数量
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        layoutManager.setReverseLayout(true);  // 反转布局方向
        layoutManager.setOrientation(GridLayoutManager.VERTICAL);  // 设置垂直方向
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return 1; // 每个项目占用1个单位宽度
            }
        });
        appList.setLayoutManager(layoutManager);
        appListAdapter = new AppListAdapter(appLRUCache);
        appList.setAdapter(appListAdapter);

        // 禁用系统输入法
        searchBox.setShowSoftInputOnFocus(false);
        searchBox.setInputType(InputType.TYPE_NULL);
        searchBox.setFocusable(true);
        searchBox.setFocusableInTouchMode(true);
        searchBox.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        // 监听搜索框文本变化
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                appListAdapter.filter(s.toString());
            }
        });

        // 为每个按钮添加点击事件
        for (int i = 0; i < keypadGrid.getChildCount(); i++) {
            View child = keypadGrid.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                button.setOnClickListener(v -> onKeypadButtonClick(button));

                // 为退格键添加长按事件
                if (button.getText().toString().contains("⌫")) {
                    button.setOnLongClickListener(v -> {
                        searchBox.setText("");
                        return true;
                    });
                }
                // 为数字 "1" 键添加长按事件以打开设置
                if (button.getText().toString().startsWith("1")) {
                    String originalText = button.getText().toString();
                    String[] lines = originalText.split("\n", 2);
                    String newFirstLine = lines[0] + " ⚙"; // 在数字 "1" 后添加图标
                    if (lines.length > 1) {
                        button.setText(newFirstLine + "\n" + lines[1]);
                    } else {
                        button.setText(newFirstLine);
                    }

                    button.setOnLongClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intent);
                        return true; // 表示事件已处理
                    });
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // It's important to load data and then set/filter the adapter.
        // Clearing searchBox here will trigger afterTextChanged -> appListAdapter.filter,
        // so the list should be populated before this.
        loadAppList(); // This calls refreshCombinedListDisplay
        fetchBookmarks(); // This also calls refreshCombinedListDisplay

        // Set text to empty to ensure a clean state and trigger initial filter correctly
        // after data might have been loaded.
        searchBox.setText("");
    }

    private void fetchBookmarks() {
        webdavService.fetchAndParseBookmarks(new WebdavService.BookmarksCallback() {
            @Override
            public void onSuccess(List<Bookmark> bookmarks) {
                Log.i(TAG, "Successfully fetched " + bookmarks.size() + " bookmarks.");
                MainActivity.this.allBookmarks.clear();
                MainActivity.this.allBookmarks.addAll(bookmarks);
                // Sort bookmarks alphabetically by name (case-insensitive)
                Collections.sort(MainActivity.this.allBookmarks, Comparator.comparing(Bookmark::getName, String.CASE_INSENSITIVE_ORDER));
                
                runOnUiThread(() -> refreshCombinedListDisplay());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to fetch bookmarks: " + e.getMessage(), e);
                MainActivity.this.allBookmarks.clear(); // Clear bookmarks on failure
                runOnUiThread(() -> {
                    refreshCombinedListDisplay();
                    // Optionally, show a toast or some other user feedback
                    // Toast.makeText(MainActivity.this, "Failed to load bookmarks", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadAppList() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        allApps.clear();

        for (ResolveInfo resolveInfo : resolveInfos) {
            String appName = resolveInfo.loadLabel(pm).toString();
            String packageName = resolveInfo.activityInfo.packageName;

            // 过滤掉自己
            if (packageName.equals("com.moonveil.t9launch")) {
                continue;
            }

            AppInfo appInfo = new AppInfo(
                    appName,
                    packageName,
                    resolveInfo.loadIcon(pm)
            );
            allApps.add(appInfo);
        }

        // 根据LRU缓存排序 (oldest first, as RecyclerView's reverseLayout will show newest at bottom)
        Collections.sort(allApps, (a1, a2) -> {
            long time1 = appLRUCache.getLastUsed(a1.getPackageName());
            long time2 = appLRUCache.getLastUsed(a2.getPackageName());
            return Long.compare(time1, time2); 
        });
        
        // As loadAppList can be called from onStart, ensure UI updates are on main thread
        // if this method itself isn't guaranteed to be on UI thread (though loadAppList usually is).
        // For safety, or if it were a background task:
        // runOnUiThread(() -> refreshCombinedListDisplay());
        // However, since onStart() and loadAppList() are on main thread, direct call is fine.
        refreshCombinedListDisplay();
    }

    // This method must be called on the UI thread if it triggers adapter changes.
    private synchronized void refreshCombinedListDisplay() {
        List<Object> combinedList = new ArrayList<>();
        // Apps are sorted by LRU (oldest first in the list)
        combinedList.addAll(allApps);
        // Bookmarks are sorted alphabetically (A-Z first in the list)
        combinedList.addAll(allBookmarks);

        if (appListAdapter != null) {
            appListAdapter.setAppList(combinedList); // This sets masterList in adapter
            // Explicitly call filter after updating the master list in the adapter.
            // This ensures the displayed list (filteredList) is correctly populated based on the new masterList
            // and current search query.
            if (searchBox != null && searchBox.getText() != null) {
                appListAdapter.filter(searchBox.getText().toString());
            } else {
                appListAdapter.filter(""); // Filter with empty string if searchBox is null
            }
        }
    }

    private void onKeypadButtonClick(MaterialButton button) {
        String buttonLabel = button.getText().toString().split("\n")[0]; // 获取按钮的标签, e.g., "1" or "1 ⚙"
        String currentText = searchBox.getText().toString();
        String buttonValue = buttonLabel;

        // 如果按钮标签以设置图标结尾，则实际值是去除图标的部分
        if (buttonLabel.endsWith(" ⚙")) {
            buttonValue = buttonLabel.substring(0, buttonLabel.length() - " ⚙".length()).trim();
        }

        // 处理特殊按键
        if (buttonValue.equals("*")) { // 使用 buttonValue 进行判断
            // 可以用作特殊功能键
            return;
        } else if (buttonLabel.equals("⌫")) { // 退格键的标签没有改变
            // 退格键
            if (currentText.length() > 0) {
                searchBox.setText(currentText.substring(0, currentText.length() - 1));
            }
            return;
        }

        // 添加数字到搜索框
        searchBox.setText(currentText + buttonValue); // 使用 buttonValue
        searchBox.setSelection(searchBox.length()); // 将光标移到末尾
    }
}
