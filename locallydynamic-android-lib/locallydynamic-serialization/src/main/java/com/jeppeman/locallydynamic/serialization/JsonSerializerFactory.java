package com.jeppeman.locallydynamic.serialization;

public class JsonSerializerFactory {
    public static JsonSerializer create() {
        return new JsonSerializerImpl();
    }
}
