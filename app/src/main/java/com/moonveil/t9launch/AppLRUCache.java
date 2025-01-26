package com.moonveil.t9launch;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class AppLRUCache {
    private static final String PREFS_NAME = "AppLRUCache";
    private static final String TIMESTAMP_PREFIX = "timestamp_";
    private static final int MAX_CACHE_SIZE = 20; // 最多保留20个应用的使用记录
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
                String packageName = entry.getKey().substring(TIMESTAMP_PREFIX.length());
                cache.put(packageName, (Long) entry.getValue());
            }
        }
    }

    public void recordUsage(String packageName) {
        long timestamp = System.currentTimeMillis();
        cache.put(packageName, timestamp);
        prefs.edit()
             .putLong(TIMESTAMP_PREFIX + packageName, timestamp)
             .apply();
             
        // 如果添加新记录后超过限制，清除最旧的一条记录
        if (cache.size() > MAX_CACHE_SIZE) {
            String oldestPackage = null;
            long oldestTime = Long.MAX_VALUE;
            
            for (Map.Entry<String, Long> entry : cache.entrySet()) {
                if (entry.getValue() < oldestTime) {
                    oldestTime = entry.getValue();
                    oldestPackage = entry.getKey();
                }
            }
            
            if (oldestPackage != null) {
                cache.remove(oldestPackage);
                prefs.edit()
                     .remove(TIMESTAMP_PREFIX + oldestPackage)
                     .apply();
            }
        }
    }

    public long getLastUsed(String packageName) {
        return cache.getOrDefault(packageName, 0L);
    }

    /**
     * Calculates the nth Fibonacci number using an iterative approach
     * @param n The position in the Fibonacci sequence (must be >= 0)
     * @return The Fibonacci number at position n
     */
    public static int fibonacci(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Input must be non-negative");
        }
        
        if (n <= 1) {
            return n;
        }
        
        int prev = 0;
        int curr = 1;
        for (int i = 2; i <= n; i++) {
            int next = prev + curr;
            prev = curr;
            curr = next;
        }
        return curr;
    }
}
