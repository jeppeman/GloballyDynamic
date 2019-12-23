package com.jeppeman.globallydynamic.net;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface Interceptor {
    Request intercept(Request request);
}