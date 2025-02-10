package com.moonveil.t9launch;

import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
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

        List<AppInfo> fullPinyinList = new ArrayList<>(); // 全拼匹配
        List<AppInfo> shortPinyinList = new ArrayList<>(); // 首字母匹配
        List<AppInfo> startsWithList = new ArrayList<>();  // 开头匹配
        List<AppInfo> containsList = new ArrayList<>();    // 包含匹配

        if (query.isEmpty()) {
            tempList.addAll(appList);
        } else {
            for (AppInfo app : appList) {
                // 全拼匹配优先级最高
                if (app.getFullT9Key().equals(query)) {
                    fullPinyinList.add(app);
                } 
                // 首字母完全匹配
                else if (app.getT9Key().equals(query)) {
                    shortPinyinList.add(app);
                }
                // 开头匹配
                else if (app.getT9Key().startsWith(query)) {
                    startsWithList.add(app);
                }
                // 包含匹配
                else if (app.getT9Key().contains(query)) {
                    containsList.add(app);
                }
                // 全拼开头匹配（新增）
                else if (app.getFullT9Key().startsWith(query)) {
                    startsWithList.add(app);
                }
                // 全拼包含匹配（新增）
                else if (app.getFullT9Key().contains(query)) {
                    containsList.add(app);
                }
            }

            // 按优先级合并列表
            tempList.addAll(fullPinyinList);
            tempList.addAll(shortPinyinList);
            tempList.addAll(startsWithList);
            tempList.addAll(containsList);
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
            }
        });

        // 长按显示菜单
        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, app);
            return true;
        });
    }

    private void showPopupMenu(View view, AppInfo app) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenuInflater().inflate(R.menu.menu_app_actions, popup.getMenu());

        // 设置菜单项点击事件
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_app_info) {
                // 打开应用信息
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + app.getPackageName()));
                view.getContext().startActivity(intent);
                return true;
            }
            return false;
        });

        popup.show();
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
