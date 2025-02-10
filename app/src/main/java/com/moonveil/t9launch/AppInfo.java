package com.moonveil.t9launch;

import android.graphics.drawable.Drawable;

import com.github.promeg.pinyinhelper.Pinyin;

import java.util.ArrayList;
import java.util.List;

public class AppInfo {
    private String appName;
    private String packageName;
    private Drawable icon;
    private String t9Key;  // 首字母T9数字序列
    private String fullT9Key;  // 全拼T9数字序列

    public AppInfo(String appName, String packageName, Drawable icon) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.t9Key = convertToShortT9Key(appName);
        this.fullT9Key = convertToFullT9Key(appName);
    }

    // 拆分首字母转换方法
    private String convertToShortT9Key(String text) {
        List<String> firstLetters = new ArrayList<>();
        for (String s : Pinyin.toPinyin(text, "|").toLowerCase().split("\\|")) {
            firstLetters.add(s.substring(0, 1));
        }
        return convertLettersToT9(String.join("", firstLetters));
    }

    // 新增全拼转换方法
    private String convertToFullT9Key(String text) {
        String pinyin = Pinyin.toPinyin(text, "").toLowerCase();
        return convertLettersToT9(pinyin);
    }

    // 通用字母转T9方法
    private String convertLettersToT9(String letters) {
        StringBuilder t9Key = new StringBuilder();
        for (char c : letters.toCharArray()) {
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

    public String getFullT9Key() {
        return fullT9Key;
    }
}
