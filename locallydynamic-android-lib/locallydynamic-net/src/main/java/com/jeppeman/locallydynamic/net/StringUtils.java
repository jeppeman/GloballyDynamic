package com.jeppeman.locallydynamic.net;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class StringUtils {
    public static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
