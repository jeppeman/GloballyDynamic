package com.jeppeman.locallydynamic.net;

public interface Interceptor {
    Request intercept(Request request);
}