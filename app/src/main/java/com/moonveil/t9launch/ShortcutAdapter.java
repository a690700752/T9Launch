package com.moonveil.t9launch;

import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ShortcutAdapter extends RecyclerView.Adapter<ShortcutAdapter.ViewHolder> {
    private List<ShortcutInfo> shortcuts;
    private OnShortcutClickListener listener;
    private LauncherApps launcherApps;

    public interface OnShortcutClickListener {
        void onShortcutClick(ShortcutInfo shortcut);
    }

    public ShortcutAdapter(LauncherApps launcherApps, OnShortcutClickListener listener) {
        this.shortcuts = new ArrayList<>();
        this.listener = listener;
        this.launcherApps = launcherApps;
    }

    public void setShortcuts(List<ShortcutInfo> shortcuts) {
        this.shortcuts = new ArrayList<>(shortcuts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shortcut, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShortcutInfo shortcut = shortcuts.get(position);
        holder.shortcutLabel.setText(shortcut.getShortLabel());
        
        // 获取快捷方式图标
        Drawable icon = launcherApps.getShortcutIconDrawable(shortcut, 0);
        if (icon != null) {
            holder.shortcutIcon.setImageDrawable(icon);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShortcutClick(shortcut);
            }
        });
    }

    @Override
    public int getItemCount() {
        return shortcuts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView shortcutIcon;
        TextView shortcutLabel;

        ViewHolder(View itemView) {
            super(itemView);
            shortcutIcon = itemView.findViewById(R.id.shortcutIcon);
            shortcutLabel = itemView.findViewById(R.id.shortcutLabel);
        }
    }
} 