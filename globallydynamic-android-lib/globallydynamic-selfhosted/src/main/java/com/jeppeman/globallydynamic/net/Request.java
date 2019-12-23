package com.jeppeman.globallydynamic.net;

import com.jeppeman.globallydynamic.serialization.JsonSerializerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Request {
    private final HttpUrl url;
    private final HttpMethod method;
    private final Headers headers;
    private final InputStream body;

    private Request(
            HttpUrl url,
            HttpMethod method,
            Headers headers,
            InputStream body) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.body = body;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public HttpUrl getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Headers getHeaders() {
        return headers;
    }

    public InputStream getBody() {
        return body;
    }

    public static class Builder {
        private final Headers headers = new Headers();
        private final Fields fields = new Fields();
        private MultiPart multiPart = null;
        private HttpUrl url = null;
        private HttpMethod method = HttpMethod.GET;
        private Object body = null;
        private boolean formUrlEncoded;

        Builder() {

        }

        Builder(Request request) {
            headers.putAll(request.headers);
            url = request.url;
            method = request.method;
            body = request.body;
        }

        public Builder setBody(Object body) {
            this.body = body;
            return this;
        }

        public Builder setMethod(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder formUrlEncoded() {
            formUrlEncoded = true;
            headers.put(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_FORM_URL_ENCODED);
            return this;
        }

        public Builder url(HttpUrl url) {
            this.url = url;
            return this;
        }

        public Builder url(String url) {
            this.url = HttpUrl.parse(url);
            return this;
        }

        public Builder addHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder addField(String name, Object value) {
            Field field = Field.builder().setName(name).setValue(value).build();
            fields.put(field.getName(), field.getValue().toString());
            return this;
        }

        public Builder setMultiPart(MultiPart multiPart) {
            this.multiPart = multiPart;
            return this;
        }

        public Builder setHeaders(Headers headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder fields(Fields fields) {
            this.fields.putAll(fields);
            return this;
        }

        public Request build() {
            HttpUrl url = this.url;
            if (url == null) {
                throw new IllegalArgumentException("Url must not be null");
            }
            InputStream inputStreamBody;
            if (multiPart != null) {
                String mediaType = multiPart.getMediaType();
                headers.put(HttpConstants.HEADER_CONTENT_TYPE, mediaType);
                inputStreamBody = multiPart.getInputStream();
            } else if (!(body instanceof InputStream)) {
                String stringBody;
                if (!fields.isEmpty()) {
                    stringBody = fields.asString();
                } else if (body != null && !(body instanceof String)) {
                    stringBody = JsonSerializerFactory.create().serialize(body);
                } else if (body != null) {
                    stringBody = body.toString();
                } else {
                    stringBody = null;
                }

                if (stringBody != null) {
                    try {
                        inputStreamBody = new ByteArrayInputStream(stringBody.getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        inputStreamBody = null;
                    }
                } else {
                    inputStreamBody = null;
                }
            } else {
                inputStreamBody = (InputStream) body;
            }

            if (!headers.containsKey(HttpConstants.HEADER_CONTENT_TYPE)) {
                headers.put(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_APPLICATION_JSON);
            }


            if (!headers.containsKey(HttpConstants.HEADER_ACCEPT)) {
                headers.put(HttpConstants.HEADER_ACCEPT, HttpConstants.MEDIA_TYPE_APPLICATION_JSON);
            }

            return new Request(
                    url,
                    method,
                    headers,
                    inputStreamBody
            );
        }

    }

    public static Builder builder() {
        return new Builder();
    }
}