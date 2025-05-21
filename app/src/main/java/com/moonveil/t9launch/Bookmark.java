package com.moonveil.t9launch;

import com.github.promeg.pinyinhelper.Pinyin;

import java.util.ArrayList;
import java.util.List;

public class Bookmark {
    private String name;
    private String url;
    private String t9Key; // T9数字序列，用于搜索

    public Bookmark(String name, String url) {
        this.name = name;
        this.url = url;
        this.t9Key = convertToT9Key(name);
    }

    private String convertToT9Key(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        List<String> firstLetters = new ArrayList<>();
        String pinyinOutput = Pinyin.toPinyin(text, "|");
        
        // Default to empty string if pinyinOutput is null to prevent NPE
        if (pinyinOutput == null) {
            pinyinOutput = "";
        }

        for (String s : pinyinOutput.toLowerCase().split("\\|")) {
            if (s != null && !s.isEmpty()) {
                firstLetters.add(s.substring(0, 1));
            }
        }

        String initials = String.join("", firstLetters);

        StringBuilder t9KeyBuilder = new StringBuilder();
        for (char c : initials.toCharArray()) {
            if (Character.isLetter(c)) {
                t9KeyBuilder.append(letterToT9Digit(c));
            } else if (Character.isDigit(c)) {
                t9KeyBuilder.append(c);
            }
        }
        return t9KeyBuilder.toString();
    }

    private char letterToT9Digit(char letter) {
        switch (Character.toLowerCase(letter)) { // Ensure consistency with lowercase
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
                return '0'; // Default for non-alphabetic chars in pinyin string
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getT9Key() {
        return t9Key;
    }

    public void setT9Key(String t9Key) {
        this.t9Key = t9Key;
    }

    // 如果需要，可以添加 equals() 和 hashCode() 方法
    // 以及 toString() 方法用于调试
}
