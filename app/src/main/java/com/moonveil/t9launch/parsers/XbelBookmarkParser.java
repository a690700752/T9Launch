package com.moonveil.t9launch.parsers;

import com.moonveil.t9launch.Bookmark;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class XbelBookmarkParser implements BookmarkParser {
    @Override
    public List<Bookmark> parse(InputStream inputStream, String baseUri) throws Exception {
        List<Bookmark> bookmarks = new ArrayList<>();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(inputStream, null); // 编码将从XML声明中自动检测

        String currentTitle = null;
        String currentHref = null;
        boolean inBookmarkTag = false;
        boolean inTitleTag = false;

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = xpp.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if ("bookmark".equalsIgnoreCase(tagName)) {
                        inBookmarkTag = true;
                        currentHref = xpp.getAttributeValue(null, "href");
                        currentTitle = ""; // 为此书签初始化标题
                    } else if (inBookmarkTag && "title".equalsIgnoreCase(tagName)) {
                        inTitleTag = true;
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (inTitleTag) {
                        String text = xpp.getText();
                        if (text != null) {
                            currentTitle += text.trim();
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if ("bookmark".equalsIgnoreCase(tagName)) {
                        if (currentHref != null && !currentHref.isEmpty() && currentTitle != null && !currentTitle.trim().isEmpty()) {
                            bookmarks.add(new Bookmark(currentTitle.trim(), currentHref));
                        }
                        inBookmarkTag = false;
                        currentHref = null;
                        currentTitle = null;
                    } else if ("title".equalsIgnoreCase(tagName)) {
                        if (inBookmarkTag) {
                            inTitleTag = false;
                        }
                    }
                    break;
            }
            eventType = xpp.next();
        }
        return bookmarks;
    }
}
