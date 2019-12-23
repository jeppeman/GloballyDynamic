package com.jeppeman.globallydynamic.serialization;

import java.util.List;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class StringUtils {
    public static String joinToString(List<?> list, String separator) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Object string : list) {
            stringBuilder.append(string).append(separator);
        }

        return stringBuilder.length() > 0
                ? stringBuilder.substring(0, stringBuilder.length() - separator.length())
                : stringBuilder.toString();
    }
}
