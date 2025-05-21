package com.moonveil.t9launch;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
    private List<Object> masterList; // Can hold AppInfo or Bookmark objects
    private List<Object> filteredList; // Can hold AppInfo or Bookmark objects
    private AppLRUCache appLRUCache;

    public AppListAdapter(AppLRUCache appLRUCache) {
        this.masterList = new ArrayList<>();
        this.filteredList = new ArrayList<>();
        this.appLRUCache = appLRUCache;
    }

    public void setAppList(List<Object> newList) {
        this.masterList = new ArrayList<>(newList); // Store the combined, sorted list
        // Initial filter will be called by text watcher or explicitly
        // For now, just apply the current filter (which might be empty)
        // This will be handled by MainActivity calling filter() after setAppList or text change
        // To ensure initial display is correct if searchBox is empty:
        filter(getCurrentQuery()); // Assuming a method to get current query or pass it
    }
    
    // Helper to get current query, replace with actual way if searchBox is accessible
    // or ensure filter is called from MainActivity after setAppList
    private String getCurrentQuery() {
        // This is a placeholder. MainActivity should call filter.
        // For example, if MainActivity.searchBox is available:
        // return MainActivity.searchBox.getText().toString();
        return ""; // Default to empty if not directly accessible
    }


    public void filter(String query) {
        filteredList.clear();
        List<Object> tempList = new ArrayList<>();

        List<Object> startsWithList = new ArrayList<>();
        List<Object> containsList = new ArrayList<>();
        List<Object> equalsList = new ArrayList<>();

        if (query.isEmpty()) {
            tempList.addAll(masterList); // Use masterList for empty query
        } else {
            for (Object item : masterList) { // Iterate over masterList
                String t9Key = null;
                if (item instanceof AppInfo) {
                    t9Key = ((AppInfo) item).getT9Key();
                } else if (item instanceof Bookmark) {
                    t9Key = ((Bookmark) item).getT9Key();
                }

                if (t9Key != null) {
                    if (t9Key.equals(query)) {
                        equalsList.add(item);
                    } else if (t9Key.startsWith(query)) {
                        startsWithList.add(item);
                    } else if (t9Key.contains(query)) {
                        containsList.add(item);
                    }
                }
            }
            // Order: Contains, then StartsWith, then Equals.
            // Adapter reverses this, so Equals will be at the bottom (highest priority).
            tempList.addAll(containsList);
            tempList.addAll(startsWithList);
            tempList.addAll(equalsList);
        }
        
        // Reverse the tempList to achieve bottom-up display (newest/highest priority at bottom)
        // when used with RecyclerView's reverseLayout=true.
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
        Object item = filteredList.get(position);
        Context context = holder.itemView.getContext();

        if (item instanceof AppInfo) {
            AppInfo app = (AppInfo) item;
            holder.appName.setText(app.getAppName());
            holder.appIcon.setImageDrawable(app.getIcon());

            holder.itemView.setOnClickListener(v -> {
                appLRUCache.recordUsage(app.getPackageName());
                Intent launchIntent = context.getPackageManager()
                        .getLaunchIntentForPackage(app.getPackageName());
                if (launchIntent != null) {
                    context.startActivity(launchIntent);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                showPopupMenuForApp(v, app);
                return true;
            });
        } else if (item instanceof Bookmark) {
            Bookmark bookmark = (Bookmark) item;
            holder.appName.setText(bookmark.getName());
            // TODO: Consider adding a specific bookmark icon in res/drawable
            holder.appIcon.setImageResource(android.R.drawable.ic_menu_compass); // Placeholder icon

            holder.itemView.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.getUrl()));
                // Add FLAG_ACTIVITY_NEW_TASK if starting from a non-Activity context,
                // but here, context is from ViewHolder, usually fine.
                context.startActivity(browserIntent);
            });

            // Long-click for bookmarks: currently no action.
            // Implement showPopupMenuForBookmark(v, bookmark) if needed.
            holder.itemView.setOnLongClickListener(null);
        }
    }

    private void showPopupMenuForApp(View view, AppInfo app) {
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
