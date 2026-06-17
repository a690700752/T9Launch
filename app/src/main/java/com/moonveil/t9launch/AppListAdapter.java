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
import androidx.recyclerview.widget.DiffUtil;
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
        this.masterList = new ArrayList<>(newList);
        // filter() will be called by the caller (MainActivity.refreshCombinedListDisplay) 
        // after setAppList to avoid duplicate filtering
    }


    public void filter(String query) {
        List<Object> newFilteredList = new ArrayList<>();

        List<Object> startsWithList = new ArrayList<>();
        List<Object> containsList = new ArrayList<>();
        List<Object> equalsList = new ArrayList<>();

        if (query.isEmpty()) {
            newFilteredList.addAll(masterList); // Use masterList for empty query
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
            newFilteredList.addAll(containsList);
            newFilteredList.addAll(startsWithList);
            newFilteredList.addAll(equalsList);
        }
        
        // Reverse the newFilteredList to achieve bottom-up display (newest/highest priority at bottom)
        // when used with RecyclerView's reverseLayout=true.
        List<Object> reversedList = new ArrayList<>(newFilteredList.size());
        for (int i = newFilteredList.size() - 1; i >= 0; i--) {
            reversedList.add(newFilteredList.get(i));
        }

        // 使用 DiffUtil 计算差异，仅更新变化的 item，避免 notifyDataSetChanged 导致的全量重建
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new AppDiffCallback(filteredList, reversedList));
        filteredList = reversedList;
        diffResult.dispatchUpdatesTo(this);
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
                appLRUCache.recordUsage(bookmark.getUrl());
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

    /**
     * DiffUtil.Callback for efficient RecyclerView updates.
     * Computes the minimal set of changes between old and new lists,
     * allowing RecyclerView to animate only changed/moved/added/removed items.
     */
    private static class AppDiffCallback extends DiffUtil.Callback {
        private final List<Object> oldList;
        private final List<Object> newList;

        AppDiffCallback(List<Object> oldList, List<Object> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);
            if (oldItem instanceof AppInfo && newItem instanceof AppInfo) {
                return ((AppInfo) oldItem).getPackageName().equals(((AppInfo) newItem).getPackageName());
            } else if (oldItem instanceof Bookmark && newItem instanceof Bookmark) {
                return ((Bookmark) oldItem).getUrl().equals(((Bookmark) newItem).getUrl());
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);
            if (oldItem instanceof AppInfo && newItem instanceof AppInfo) {
                AppInfo oldApp = (AppInfo) oldItem;
                AppInfo newApp = (AppInfo) newItem;
                return oldApp.getAppName().equals(newApp.getAppName())
                        && oldApp.getPackageName().equals(newApp.getPackageName());
            } else if (oldItem instanceof Bookmark && newItem instanceof Bookmark) {
                Bookmark oldBm = (Bookmark) oldItem;
                Bookmark newBm = (Bookmark) newItem;
                return oldBm.getName().equals(newBm.getName())
                        && oldBm.getUrl().equals(newBm.getUrl());
            }
            return false;
        }
    }
} 
