package com.moonveil.t9launch;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.Map;

public class AppDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "app_stats.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "app_clicks";
    private static final String COLUMN_PACKAGE_NAME = "package_name";
    private static final String COLUMN_CLICK_COUNT = "click_count";

    public AppDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_PACKAGE_NAME + " TEXT PRIMARY KEY, " +
                COLUMN_CLICK_COUNT + " INTEGER DEFAULT 0)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public int getClickCount(String packageName) {
        SQLiteDatabase db = this.getReadableDatabase();
        int clickCount = 0;

        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_CLICK_COUNT},
                COLUMN_PACKAGE_NAME + " = ?",
                new String[]{packageName},
                null, null, null);

        if (cursor.moveToFirst()) {
            clickCount = cursor.getInt(0);
        }
        cursor.close();

        return clickCount;
    }

    public Map<String, Integer> getAllClickCounts() {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, Integer> clickCounts = new HashMap<>();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_PACKAGE_NAME, COLUMN_CLICK_COUNT}, null, null, null, null, null);
        while (cursor.moveToNext()) {
            clickCounts.put(cursor.getString(0), cursor.getInt(1));
        }
        cursor.close();
        return clickCounts;
    }

    public void incrementClickCount(String packageName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        int currentCount = getClickCount(packageName);
        
        values.put(COLUMN_PACKAGE_NAME, packageName);
        values.put(COLUMN_CLICK_COUNT, currentCount + 1);

        db.insertWithOnConflict(TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }
} 