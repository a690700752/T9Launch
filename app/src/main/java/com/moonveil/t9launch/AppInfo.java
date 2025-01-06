package com.moonveil.t9launch;

import android.graphics.drawable.Drawable;

import com.github.promeg.pinyinhelper.Pinyin;

import java.util.ArrayList;
import java.util.List;

public class AppInfo {
    private String appName;
    private String packageName;
    private Drawable icon;
    private String t9Key;  // T9数字序列

    public AppInfo(String appName, String packageName, Drawable icon) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.t9Key = convertToT9Key(appName);
    }

    private String convertToT9Key(String text) {
        // 先将文本转换为拼音（不带声调）
        List<String> firstLetters = new ArrayList<>();
        for (String s : Pinyin.toPinyin(text, "|").toLowerCase().split("\\|")) {
            firstLetters.add(s.substring(0, 1));
        }

        String pinyin = String.join("", firstLetters);

        StringBuilder t9Key = new StringBuilder();

        // 将拼音转换为T9键码
        for (char c : pinyin.toCharArray()) {
            if (Character.isLetter(c)) {
                t9Key.append(letterToT9Digit(c));
            } else if (Character.isDigit(c)) {
                t9Key.append(c);
            }
        }

        return t9Key.toString();
    }

    private char letterToT9Digit(char letter) {
        switch (letter) {
            case 'a':
            case 'b':
            case 'c':
                return '2';
            case 'd':
            case 'e':
            case 'f':
                return '3';
            case 'g':
            case 'h':
            case 'i':
                return '4';
            case 'j':
            case 'k':
            case 'l':
                return '5';
            case 'm':
            case 'n':
            case 'o':
                return '6';
            case 'p':
            case 'q':
            case 'r':
            case 's':
                return '7';
            case 't':
            case 'u':
            case 'v':
                return '8';
            case 'w':
            case 'x':
            case 'y':
            case 'z':
                return '9';
            default:
                return '0';
        }
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getT9Key() {
        return t9Key;
    }
}