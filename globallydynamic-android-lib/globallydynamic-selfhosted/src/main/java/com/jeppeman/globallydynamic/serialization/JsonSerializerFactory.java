package com.jeppeman.globallydynamic.serialization;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class JsonSerializerFactory {
    public static JsonSerializer create() {
        return new JsonSerializerImpl();
    }
}
