package com.jeppeman.globallydynamic.globalsplitinstall;

import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;

class StringUtils {
    static String toBase64(@NonNull String string) {
        try {
            return Base64.encodeToString(
                    string.getBytes("UTF-8"),
                    Base64.NO_WRAP | Base64.URL_SAFE | Base64.NO_PADDING
            );
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static String fromFeatureInfo(@NonNull FeatureInfo featureInfo) {
        if (featureInfo.name == null
                && featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED) {
            int majorGlEsVersion = ((featureInfo.reqGlEsVersion & 0xffff0000) >> 16);
            return "reqGlEsVersion=" + Integer.toHexString(majorGlEsVersion);
        } else if (featureInfo.name == null) {
            return "reqGlEsVersion=1";
        } else {
            return featureInfo.name;
        }
    }

    static String fromIntent(Intent intent) {
        StringBuilder builder;
        if (intent != null && intent.getExtras() != null && intent.getExtras().keySet() != null) {
            builder = new StringBuilder("Intent(action=")
                    .append(intent.getAction())
                    .append(", data=")
                    .append(intent.getData())
                    .append(", extras=");
            for (String key : intent.getExtras().keySet()) {
                builder.append(key)
                        .append("=")
                        .append(intent.getExtras().get(key))
                        .append(", ");
            }

            builder.append(")");
        } else {
            builder = new StringBuilder("null");
        }

        return builder.toString();
    }
}