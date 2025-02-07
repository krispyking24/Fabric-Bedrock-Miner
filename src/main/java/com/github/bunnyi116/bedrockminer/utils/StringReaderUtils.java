package com.github.bunnyi116.bedrockminer.utils;

import com.mojang.brigadier.StringReader;

public class StringReaderUtils {
    public static String readUnquotedString(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead()) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }
}
