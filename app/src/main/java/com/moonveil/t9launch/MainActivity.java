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
                // Sorting will be done in refreshCombinedListDisplay for the combined list
                
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

        // Sorting will be done in refreshCombinedListDisplay for the combined list
        
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
        combinedList.addAll(allApps);
        combinedList.addAll(allBookmarks);

        // Sort the combined list.
        // Given RecyclerView's reverseLayout=true, items that are "greater" in the sort
        // order (come later in the sorted list) will appear at the top of the screen.
        // Desired display order:
        // 1. Used apps (most recent first on screen).
        // 2. Then, unused apps and bookmarks (alphabetically A-Z on screen).
        Collections.sort(combinedList, (o1, o2) -> {
            long time1 = 0;
            String name1 = "";
            boolean o1HasRecentUsage = false;

            if (o1 instanceof AppInfo) {
                AppInfo app1 = (AppInfo) o1;
                name1 = app1.getAppName();
                time1 = appLRUCache.getLastUsed(app1.getPackageName());
            } else if (o1 instanceof Bookmark) {
                Bookmark bookmark1 = (Bookmark) o1;
                name1 = bookmark1.getName();
                // For bookmarks, use their URL as the unique identifier in the LRU cache.
                // Ensure recordUsage(bookmark.getUrl()) is called when a bookmark is opened.
                time1 = appLRUCache.getLastUsed(bookmark1.getUrl());
            }
            if (time1 > 0) {
                o1HasRecentUsage = true;
            }

            long time2 = 0;
            String name2 = "";
            boolean o2HasRecentUsage = false;

            if (o2 instanceof AppInfo) {
                AppInfo app2 = (AppInfo) o2;
                name2 = app2.getAppName();
                time2 = appLRUCache.getLastUsed(app2.getPackageName());
            } else if (o2 instanceof Bookmark) {
                Bookmark bookmark2 = (Bookmark) o2;
                name2 = bookmark2.getName();
                time2 = appLRUCache.getLastUsed(bookmark2.getUrl());
            }
            if (time2 > 0) {
                o2HasRecentUsage = true;
            }

            // Rule 1: Items with recent usage are "greater" than non-recent items.
            // "Greater" items come later in the sorted list, appearing at the top with reverseLayout=true.
            if (o1HasRecentUsage && !o2HasRecentUsage) {
                return 1; // o1 is "greater"
            }
            if (!o1HasRecentUsage && o2HasRecentUsage) {
                return -1; // o1 is "smaller"
            }

            // Rule 2: If both items have recent usage, sort by time.
            // Long.compare sorts in ascending order (older timestamps first in the list).
            // With reverseLayout=true, newer items (larger timestamps) appear at the top.
            if (o1HasRecentUsage && o2HasRecentUsage) {
                int timeCompare = Long.compare(time1, time2);
                if (timeCompare != 0) {
                    return timeCompare;
                }
                // Fallback to name sort if times are identical.
                // Ascending name sort (A-Z in list) means Z appears at top of this equally-timed group.
                return name1.compareToIgnoreCase(name2);
            }

            // Rule 3: If neither item has recent usage, sort by name.
            // Ascending name sort (A-Z in list) means Z appears at top of this non-recent group.
            return name1.compareToIgnoreCase(name2);
        });

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
