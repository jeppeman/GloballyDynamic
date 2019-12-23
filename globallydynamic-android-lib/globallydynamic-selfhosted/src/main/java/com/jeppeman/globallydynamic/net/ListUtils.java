package com.jeppeman.globallydynamic.net;

import java.util.List;
import java.util.Set;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ListUtils {
    public static String toString(List<?> list, boolean includeBrackets, boolean spaceAfterComma) {
        StringBuilder stringBuilder = new StringBuilder(includeBrackets
                ? "["
                : "");

        for (Object item : list) {
            stringBuilder.append(item.toString()).append(",");
            if (spaceAfterComma) {
                stringBuilder.append(" ");
            }
        }

        int charsToRemove = includeBrackets && stringBuilder.length() > 1
                ? spaceAfterComma
                ? 2
                : 1
                : !includeBrackets && stringBuilder.length() > 0
                ? spaceAfterComma
                ? 2
                : 1
                : 0;

        stringBuilder = new StringBuilder(stringBuilder.substring(0,
                stringBuilder.length() - charsToRemove));

        return stringBuilder.append(includeBrackets
                ? "]"
                : "").toString();
    }

    public static String toString(Set<?> set) {
        StringBuilder stringBuilder = new StringBuilder("[");

        for (Object item : set) {
            stringBuilder.append(item.toString()).append(",");
        }

        if (stringBuilder.length() > 1) {
            stringBuilder = new StringBuilder(stringBuilder.substring(0,
                    stringBuilder.length() - 1));
        }

        return stringBuilder.append("]").toString();
    }

    public static String toString(List<?> list, boolean includeBrackets) {
        return toString(list, includeBrackets, false);
    }

    public static String toString(List<?> list) {
        return toString(list, true, false);
    }
}
