package com.jeppeman.globallydynamic.serialization.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.RestrictTo;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public @interface JsonSerialize {
    String value();
}