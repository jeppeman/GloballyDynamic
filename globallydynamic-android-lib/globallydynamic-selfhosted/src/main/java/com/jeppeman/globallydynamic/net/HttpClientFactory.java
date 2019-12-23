package com.jeppeman.globallydynamic.net;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class HttpClientFactory {
    public static HttpClient.Builder builder() {
        return new HttpClientImpl.BuilderImpl();
    }
}
