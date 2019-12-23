package com.jeppeman.locallydynamic.serialization;

public class JsonDeserializerFactory {
    public static JsonDeserializer create() {
        return new JsonDeserializerImpl();
    }
}