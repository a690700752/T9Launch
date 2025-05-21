package com.moonveil.t9launch.parsers;

import com.moonveil.t9launch.Bookmark;
import java.io.InputStream;
import java.util.List;

public interface BookmarkParser {
    List<Bookmark> parse(InputStream inputStream, String baseUri) throws Exception;
}
