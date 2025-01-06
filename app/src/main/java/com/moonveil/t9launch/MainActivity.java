package com.moonveil.t9launch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import com.github.promeg.pinyinhelper.Pinyin;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText searchBox;
    private AppListAdapter appListAdapter;
    private List<AppInfo> allApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchBox = findViewById(R.id.searchBox);
        GridLayout keypadContainer = findViewById(R.id.keypadContainer);
        RecyclerView appList = findViewById(R.id.appList);

        // 设置RecyclerView为网格布局
        int spanCount = 4; // 每行显示的应用数量
        GridLayoutManager layoutManager = new GridLayoutManager(this, spanCount);
        layoutManager.setReverseLayout(true);  // 反转布局方向
        layoutManager.setOrientation(GridLayoutManager.VERTICAL);  // 设置垂直方向
        appList.setLayoutManager(layoutManager);
        appListAdapter = new AppListAdapter();
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                appListAdapter.filter(s.toString());
            }
        });

        // 为每个按钮添加点击事件
        for (int i = 0; i < keypadContainer.getChildCount(); i++) {
            View child = keypadContainer.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setOnClickListener(v -> onKeypadButtonClick(button));
                
                // 为退格键添加长按事件
                if (button.getText().toString().contains("⌫")) {
                    button.setOnLongClickListener(v -> {
                        searchBox.setText("");
                        return true;
                    });
                }
            }
        }

        // 加载应用列表
        loadAppList();
    }

    private void loadAppList() {
        PackageManager pm = getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        allApps = new ArrayList<>();

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

        appListAdapter.setAppList(allApps);
    }

    private void onKeypadButtonClick(Button button) {
        String buttonText = button.getText().toString().split("\n")[0]; // 获取按钮的数字
        String currentText = searchBox.getText().toString();
        
        // 处理特殊按键
        if (buttonText.equals("*")) {
            // 可以用作特殊功能键
            return;
        } else if (buttonText.equals("⌫")) {
            // 退格键
            if (currentText.length() > 0) {
                searchBox.setText(currentText.substring(0, currentText.length() - 1));
            }
            return;
        }

        // 添加数字到搜索框
        searchBox.setText(currentText + buttonText);
        searchBox.setSelection(searchBox.length()); // 将光标移到末尾
    }
}