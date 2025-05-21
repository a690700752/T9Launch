package com.moonveil.t9launch;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class AppLRUCache {
    private static final String PREFS_NAME = "AppLRUCache";
    private static final String TIMESTAMP_PREFIX = "timestamp_";
    private static final int MAX_CACHE_SIZE = 20; // 最多保留20个条目(应用或书签)的使用记录
    private SharedPreferences prefs;
    private Map<String, Long> cache;

    public AppLRUCache(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cache = new HashMap<>();
        loadFromPrefs();
    }

    private void loadFromPrefs() {
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith(TIMESTAMP_PREFIX)) {
                String identifier = entry.getKey().substring(TIMESTAMP_PREFIX.length());
                cache.put(identifier, (Long) entry.getValue());
            }
        }
    }

    public void recordUsage(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            // Avoid storing entries with null or empty identifiers
            return;
        }
        long timestamp = System.currentTimeMillis();
        cache.put(identifier, timestamp);
        prefs.edit()
             .putLong(TIMESTAMP_PREFIX + identifier, timestamp)
             .apply();
             
        // 如果添加新记录后超过限制，清除最旧的一条记录
        if (cache.size() > MAX_CACHE_SIZE) {
            String oldestIdentifier = null;
            long oldestTime = Long.MAX_VALUE;
            
            for (Map.Entry<String, Long> entry : cache.entrySet()) {
                if (entry.getValue() < oldestTime) {
                    oldestTime = entry.getValue();
                    oldestIdentifier = entry.getKey();
                }
            }
            
            if (oldestIdentifier != null) {
                cache.remove(oldestIdentifier);
                prefs.edit()
                     .remove(TIMESTAMP_PREFIX + oldestIdentifier)
                     .apply();
            }
        }
    }

    public long getLastUsed(String identifier) {
        if (identifier == null) {
            return 0L;
        }
        return cache.getOrDefault(identifier, 0L);
    }
}
