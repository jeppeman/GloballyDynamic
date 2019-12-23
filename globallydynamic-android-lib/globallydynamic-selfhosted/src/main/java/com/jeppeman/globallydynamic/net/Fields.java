package com.jeppeman.globallydynamic.net;

import java.util.LinkedHashMap;
import java.util.Map;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Fields extends LinkedHashMap<String, String> {
    public String asString() {
        StringBuilder stringBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : entrySet()) {
            stringBuilder.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue())
                    .append("&");
        }

        return stringBuilder.length() > 0
                ? stringBuilder.substring(0, stringBuilder.length() - 1)
                : stringBuilder.toString();
    }
}
