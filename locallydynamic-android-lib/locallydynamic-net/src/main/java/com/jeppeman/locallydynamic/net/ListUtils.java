package com.jeppeman.locallydynamic.net;

import java.util.List;

public class ListUtils {
    public static String toString(List<?> list, boolean includeBrackets) {
        StringBuilder stringBuilder = new StringBuilder(includeBrackets
                ? "["
                : "");

        for (Object item : list) {
            stringBuilder.append(item.toString()).append(",");
        }

        if (includeBrackets && stringBuilder.length() > 1
                || (!includeBrackets && stringBuilder.length() > 0)) {
            stringBuilder = new StringBuilder(stringBuilder.substring(0,
                    stringBuilder.length() - 1));
        }

        return stringBuilder.append(includeBrackets
                ? "]"
                : "").toString();
    }

    public static String toString(List<?> list) {
        return toString(list, true);
    }
}
