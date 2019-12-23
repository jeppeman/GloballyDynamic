package com.jeppeman.globallydynamic.net;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class StringUtils {
    public static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
