package com.moonveil.t9launch.parsers;

import com.moonveil.t9launch.Bookmark;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HtmlBookmarkParser implements BookmarkParser {
    @Override
    public List<Bookmark> parse(InputStream inputStream, String baseUri) throws Exception {
        List<Bookmark> bookmarks = new ArrayList<>();
        Document doc = Jsoup.parse(inputStream, "UTF-8", baseUri);
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.attr("abs:href");
            String name = link.text();

            if (name != null && !name.trim().isEmpty() &&
                href != null && !href.trim().isEmpty() &&
                !href.toLowerCase().startsWith("javascript:")) {
                bookmarks.add(new Bookmark(name.trim(), href.trim()));
            }
        }
        return bookmarks;
    }
}
