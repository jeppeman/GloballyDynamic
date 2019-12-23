package com.jeppeman.globallydynamic.serialization;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class JsonObject  extends JsonElement {
    private Map<String, JsonElement> keyValuePairs = new LinkedHashMap<String, JsonElement>();

    public JsonElement get(String key) {
        return keyValuePairs.get(key);
    }

    public boolean containsKey(String key) {
        return keyValuePairs.containsKey(key);
    }

    public JsonObject put(String key, JsonElement value) {
        keyValuePairs.put(key, value);
        return this;
    }

    public JsonObject remove(String key) {
        keyValuePairs.remove(key);
        return this;
    }

    public Set<String> getKeys() {
        return keyValuePairs.keySet();
    }

    public Collection<JsonElement> getValues() {
        return keyValuePairs.values();
    }

    public Set<Map.Entry<String, JsonElement>> getEntries() {
        return keyValuePairs.entrySet();
    }

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder("{");

        for (Map.Entry<String, JsonElement> entry : getEntries()) {
            stringBuilder.append("\"")
                    .append(entry.getKey())
                    .append("\"")
                    .append(":")
                    .append(entry.getValue())
                    .append(",");
        }

        if (stringBuilder.length() > 1) {
            stringBuilder = new StringBuilder(
                    stringBuilder.substring(0, stringBuilder.length() - 1));
        }

        return stringBuilder.append("}").toString();
    }
}