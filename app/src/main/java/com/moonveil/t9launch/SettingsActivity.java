package com.moonveil.t9launch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText etWebdavUrl, etWebdavUsername, etWebdavPassword, etBookmarkFilename;
    private Button btnSaveSettings;

    public static final String PREFS_SETTINGS_NAME = "T9LaunchSettings";
    public static final String KEY_WEBDAV_URL = "webdav_url";
    public static final String KEY_WEBDAV_USERNAME = "webdav_username";
    public static final String KEY_WEBDAV_PASSWORD = "webdav_password";
    public static final String KEY_BOOKMARK_FILENAME = "bookmark_filename";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("设置");
        }

        etWebdavUrl = findViewById(R.id.et_webdav_url);
        etWebdavUsername = findViewById(R.id.et_webdav_username);
        etWebdavPassword = findViewById(R.id.et_webdav_password);
        etBookmarkFilename = findViewById(R.id.et_bookmark_filename);
        btnSaveSettings = findViewById(R.id.btn_save_settings);

        loadSettings();

        btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS_NAME, Context.MODE_PRIVATE);
        etWebdavUrl.setText(prefs.getString(KEY_WEBDAV_URL, ""));
        etWebdavUsername.setText(prefs.getString(KEY_WEBDAV_USERNAME, ""));
        etWebdavPassword.setText(prefs.getString(KEY_WEBDAV_PASSWORD, ""));
        etBookmarkFilename.setText(prefs.getString(KEY_BOOKMARK_FILENAME, "bookmarks.html"));
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_SETTINGS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_WEBDAV_URL, etWebdavUrl.getText().toString().trim());
        editor.putString(KEY_WEBDAV_USERNAME, etWebdavUsername.getText().toString().trim());
        editor.putString(KEY_WEBDAV_PASSWORD, etWebdavPassword.getText().toString()); // 密码通常不 trim
        editor.putString(KEY_BOOKMARK_FILENAME, etBookmarkFilename.getText().toString().trim());
        editor.apply();
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        finish(); // 保存后关闭设置页面
    }
}
