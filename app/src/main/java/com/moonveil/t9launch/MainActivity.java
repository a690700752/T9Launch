package com.moonveil.t9launch;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;

public class MainActivity extends AppCompatActivity {
    private EditText searchBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchBox = findViewById(R.id.searchBox);
        GridLayout keypadContainer = findViewById(R.id.keypadContainer);

        // 为每个按钮添加点击事件
        for (int i = 0; i < keypadContainer.getChildCount(); i++) {
            View child = keypadContainer.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setOnClickListener(v -> onKeypadButtonClick(button));
            }
        }
    }

    private void onKeypadButtonClick(Button button) {
        String buttonText = button.getText().toString().split("\n")[0]; // 获取按钮的数字
        String currentText = searchBox.getText().toString();
        
        // 处理特殊按键
        if (buttonText.equals("*")) {
            // 可以用作特殊功能键
            return;
        } else if (buttonText.equals("#")) {
            // 可以用作删除键
            if (currentText.length() > 0) {
                searchBox.setText(currentText.substring(0, currentText.length() - 1));
            }
            return;
        }

        // 添加数字到搜索框
        searchBox.setText(currentText + buttonText);
        searchBox.setSelection(searchBox.length()); // 将光标移到末尾
    }
}