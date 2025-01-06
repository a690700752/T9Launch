package com.moonveil.t9launch;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {
    private List<AppInfo> appList;
    private List<AppInfo> filteredList;
    private AppLRUCache appLRUCache;

    public AppListAdapter(AppLRUCache appLRUCache) {
        this.appList = new ArrayList<>();
        this.filteredList = new ArrayList<>();
        this.appLRUCache = appLRUCache;
    }

    public void setAppList(List<AppInfo> appList) {
        this.appList = new ArrayList<>(appList);
        this.filteredList.clear();
        // 反转列表顺序以保持从底部开始的显示
        for (int i = appList.size() - 1; i >= 0; i--) {
            this.filteredList.add(appList.get(i));
        }
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        List<AppInfo> tempList = new ArrayList<>();
        
        if (query.isEmpty()) {
            tempList.addAll(appList);
        } else {
            for (AppInfo app : appList) {
                // 使用T9键匹配
                if (app.getT9Key().startsWith(query)) {
                    tempList.add(app);
                }
            }
        }
        
        // 反转列表顺序以保持从底部开始的显示
        for (int i = tempList.size() - 1; i >= 0; i--) {
            filteredList.add(tempList.get(i));
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = filteredList.get(position);
        holder.appName.setText(app.getAppName());
        holder.appIcon.setImageDrawable(app.getIcon());
        
        // 点击启动应用
        holder.itemView.setOnClickListener(v -> {
            appLRUCache.recordUsage(app.getPackageName());
            
            Intent launchIntent = v.getContext().getPackageManager()
                    .getLaunchIntentForPackage(app.getPackageName());
            if (launchIntent != null) {
                v.getContext().startActivity(launchIntent);
                if (v.getContext() instanceof MainActivity) {
                    ((MainActivity) v.getContext()).finish();
                }
            }
        });

        // 长按显示应用详情
        holder.itemView.setOnLongClickListener(v -> {
            showAppDetails(v, app);
            return true;
        });
    }

    private void showAppDetails(View view, AppInfo app) {
        Dialog dialog = new Dialog(view.getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app_details);

        // 设置应用信息
        ImageView appIcon = dialog.findViewById(R.id.appIcon);
        TextView appName = dialog.findViewById(R.id.appName);
        TextView packageName = dialog.findViewById(R.id.packageName);
        RecyclerView shortcutsList = dialog.findViewById(R.id.shortcutsList);
        MaterialButton btnAppInfo = dialog.findViewById(R.id.btnAppInfo);
        MaterialButton btnClose = dialog.findViewById(R.id.btnClose);

        appIcon.setImageDrawable(app.getIcon());
        appName.setText(app.getAppName());
        packageName.setText(app.getPackageName());

        // 获取 LauncherApps 服务
        LauncherApps launcherApps = view.getContext().getSystemService(LauncherApps.class);

        // 设置快捷方式列表
        shortcutsList.setLayoutManager(new LinearLayoutManager(view.getContext()));
        ShortcutAdapter shortcutAdapter = new ShortcutAdapter(launcherApps, shortcut -> {
            try {
                // 处理快捷方式点击
                UserHandle user = android.os.Process.myUserHandle();
                launcherApps.startShortcut(shortcut, null, null);
                dialog.dismiss();
                if (view.getContext() instanceof MainActivity) {
                    ((MainActivity) view.getContext()).finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        shortcutsList.setAdapter(shortcutAdapter);

        // 获取应用的快捷方式
        try {
            List<ShortcutInfo> shortcuts = null;
            if (launcherApps.hasShortcutHostPermission()) {
                LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery()
                        .setPackage(app.getPackageName())
                        .setQueryFlags(
                                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED |
                                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC |
                                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                        );
                shortcuts = launcherApps.getShortcuts(query, android.os.Process.myUserHandle());
            }
            
            if (shortcuts != null && !shortcuts.isEmpty()) {
                shortcutAdapter.setShortcuts(shortcuts);
                shortcutsList.setVisibility(View.VISIBLE);
            } else {
                shortcutsList.setVisibility(View.GONE);
                dialog.findViewById(R.id.shortcutsTitle).setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            shortcutsList.setVisibility(View.GONE);
            dialog.findViewById(R.id.shortcutsTitle).setVisibility(View.GONE);
        }

        // 设置按钮点击事件
        btnAppInfo.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + app.getPackageName()));
            view.getContext().startActivity(intent);
            dialog.dismiss();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;

        ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
        }
    }
} 