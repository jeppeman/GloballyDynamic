package com.jeppeman.globallydynamic.net;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Headers extends HashMap<String, List<String>> {
    @Override
    public List<String> get(Object objectKey) {
        String key = objectKey.toString();
        for (Map.Entry<String, List<String>> entry : entrySet()) {
            if (entry.getKey().toLowerCase().equals(key.toLowerCase())) {
                return entry.getValue();
            }
        }

        return null;
    }

    @Override
    public boolean containsKey(Object objectKey) {
        String key = objectKey.toString();
        for (Map.Entry<String, List<String>> entry : entrySet()) {
            if (entry.getKey().toLowerCase().equals(key.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    public void put(String key, String value) {
        if (containsKey(key)) {
            get(key).add(value);
        } else {
            List<String> list = new ArrayList<String>();
            list.add(value);
            put(key, list);
        }
    }
}