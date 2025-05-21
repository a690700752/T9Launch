package com.moonveil.t9launch;

public class Bookmark {
    private String name;
    private String url;
    private String t9Key; // T9数字序列，用于搜索

    public Bookmark(String name, String url) {
        this.name = name;
        this.url = url;
        // 注意：T9 Key 的转换逻辑通常在 AppInfo 或类似类中，
        // 这里可以暂时留空或在需要时从外部设置。
        // 为了简单起见，我们可以在这里添加一个基本的转换，
        // 或者让使用 Bookmark 的类负责填充 t9Key。
        // 此处暂时不直接计算 T9 Key，留给使用者处理。
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
