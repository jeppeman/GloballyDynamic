package com.jeppeman.locallydynamic.serialization;

public abstract class JsonElement {
    public JsonObject asJsonObject() {
        if (this instanceof JsonObject) {
            return (JsonObject) this;
        }

        throw new IllegalStateException("Element is not of type JsonObject");
    }

    public JsonArray asJsonArray() {
        if (this instanceof JsonArray) {
            return (JsonArray) this;
        }

        throw new IllegalStateException("Element is not of type JsonArray");
    }

    public JsonPrimitive asJsonPrimitive() {
        if (this instanceof JsonPrimitive) {
            return (JsonPrimitive) this;
        }

        throw new IllegalStateException("Element is not of type JsonPrimitive");
    }

    public JsonNull asJsonNull() {
        if (this instanceof JsonNull) {
            return (JsonNull) this;
        }

        throw new IllegalStateException("Element is not of type JsonNull");
    }
}