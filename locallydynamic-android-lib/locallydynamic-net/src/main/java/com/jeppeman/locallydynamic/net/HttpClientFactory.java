package com.jeppeman.locallydynamic.net;

public class HttpClientFactory {
    public static HttpClient.Builder builder() {
        return new HttpClientImpl.BuilderImpl();
    }
}
