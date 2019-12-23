package com.jeppeman.globallydynamic.net;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Response<T> {
    private final int code;
    private final T body;
    private final String errorBody;
    private final Headers headers;
    private final Request request;

    private Response(
            int code,
            T body,
            String errorBody,
            Headers headers,
            Request request) {
        this.code = code;
        this.body = body;
        this.errorBody = errorBody;
        this.headers = headers;
        this.request = request;
    }

    public boolean isSuccessful() {
        return code >= 200 && code <= 299;
    }

    public int getCode() {
        return code;
    }

    public T getBody() {
        return body;
    }

    public String getErrorBody() {
        return errorBody;
    }

    public Headers getHeaders() {
        return headers;
    }

    public Request getRequest() {
        return request;
    }

    public static class Builder<T> {
        private int code;
        private T body;
        private String errorBody;
        private Headers headers;
        private Request request;

        public Builder<T> setCode(int code) {
            this.code = code;
            return this;
        }

        public Builder<T> setBody(T body) {
            this.body = body;
            return this;
        }

        public Builder<T> setErrorBody(String errorBody) {
            this.errorBody = errorBody;
            return this;
        }

        public Builder<T> setHeaders(Headers headers) {
            this.headers = headers;
            return this;
        }

        public Builder<T> setRequest(Request request) {
            this.request = request;
            return this;
        }

        public Response<T> build() {
            return new Response<T>(
                    code,
                    body,
                    errorBody,
                    headers != null ? headers : new Headers(),
                    request
            );
        }
    }

    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }
}