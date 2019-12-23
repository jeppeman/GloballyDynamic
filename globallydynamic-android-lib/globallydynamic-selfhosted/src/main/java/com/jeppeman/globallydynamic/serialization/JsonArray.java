package com.jeppeman.globallydynamic.serialization;

import java.util.LinkedList;
import java.util.List;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class JsonArray extends JsonElement {
    private List<JsonElement> values = new LinkedList<JsonElement>();

    public int length() {
        return values.size();
    }

    public JsonElement get(int index) {
        return values.get(index);
    }

    public void set(int index, JsonElement value) {
        values.set(index, value);
    }

    public JsonArray push(JsonElement value) {
        values.add(value);
        return this;
    }

    public JsonArray remove(JsonElement value) {
        values.remove(value);
        return this;
    }

    public JsonArray removeAt(int index) {
        values.remove(index);
        return this;
    }

    public List<JsonElement> getValues() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("[");

        for (JsonElement jsonElement : values) {
            stringBuilder.append(jsonElement.toString())
                    .append(",");
        }

        if (stringBuilder.length() > 1) {
            stringBuilder = new StringBuilder(
                    stringBuilder.substring(0, stringBuilder.length() - 1));
        }

        return stringBuilder.append("]").toString();
    }
}