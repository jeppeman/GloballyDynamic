package com.jeppeman.globallydynamic.serialization;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class JsonDeserializerFactory {
    public static JsonDeserializer create() {
        return new JsonDeserializerImpl();
    }
}