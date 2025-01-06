package com.moonveil.t9launch;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
        
        holder.itemView.setOnClickListener(v -> {
            // 记录应用使用
            appLRUCache.recordUsage(app.getPackageName());
            
            Intent launchIntent = v.getContext().getPackageManager()
                    .getLaunchIntentForPackage(app.getPackageName());
            if (launchIntent != null) {
                v.getContext().startActivity(launchIntent);
                // 销毁T9启动器
                if (v.getContext() instanceof MainActivity) {
                    ((MainActivity) v.getContext()).finish();
                }
            }
        });
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