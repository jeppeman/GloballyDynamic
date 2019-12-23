package com.jeppeman.locallydynamic.net;

public enum HttpMethod {
    GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");

    private final String value;

    HttpMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}