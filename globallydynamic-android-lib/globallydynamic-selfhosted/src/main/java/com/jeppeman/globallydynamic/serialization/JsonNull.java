package com.jeppeman.globallydynamic.serialization;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class JsonNull extends JsonElement {
    @Override
    public String toString() {
        return "null";
    }
}