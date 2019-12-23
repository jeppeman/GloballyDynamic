package com.jeppeman.locallydynamic.net;

import com.google.common.collect.Lists;
import com.jeppeman.locallydynamic.serialization.annotations.JsonSerialize;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class RequestBuilderTest {
    @Test
    public void whenFormUrlEncodedCalled_build_shouldGenerateRequestObjectWithFormUrlEncodedHeader() {
        Request request = Request.builder()
                .url("http://valid.url")
                .formUrlEncoded()
                .build();

        assertThat(request.getHeaders().get(HttpConstants.HEADER_CONTENT_TYPE)).isEqualTo(
                Lists.newArrayList(HttpConstants.MEDIA_TYPE_FORM_URL_ENCODED)
        );
    }

    @Test
    public void whenNoContentTypeOrAcceptIsProvided_build_shouldGenerateRequestObjectWithJsonContentTypeAndAccept() {
        Request request = Request.builder()
                .url("http://valid.url")
                .build();

        assertThat(request.getHeaders().get(HttpConstants.HEADER_CONTENT_TYPE)).isEqualTo(
                Lists.newArrayList(
                        HttpConstants.MEDIA_TYPE_APPLICATION_JSON
                )
        );
        assertThat(request.getHeaders().get(HttpConstants.HEADER_ACCEPT)).isEqualTo(
                Lists.newArrayList(
                        HttpConstants.MEDIA_TYPE_APPLICATION_JSON
                )
        );
    }

    @Test
    public void whenBodyIsNotString_build_shouldSerializeBodyToString() {
        Request request = Request.builder()
                .url("http://valid.url")
                .setBody(new TestClass("hej"))
                .build();

        assertThat(StreamUtils.readString(request.getBody())).isEqualTo("{\"a\":\"hej\"}");
    }

    @Test
    public void whenFieldsArePresent_build_shouldGenerateFormUrlEncodedBody() {
        Request request = Request.builder()
                .url("http://valid.url")
                .addField("x", "http://123.com")
                .addField("a", "abc")
                .build();

        assertThat(StreamUtils.readString(request.getBody())).isEqualTo("x=" + StringUtils.urlEncode("http://123.com") + "&a=abc");
    }

    @Test
    public void whenHeadersArePresent_build_shouldAddThemToGeneratedObject() {
        Request request = Request.builder()
                .url("http://valid.url")
                .addHeader("a", "b")
                .build();

        assertThat(request.getHeaders().get("a")).isEqualTo(Lists.newArrayList("b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenUrlIsMissing_build_shouldThrow() {
        Request.builder().build();
    }

    @Test
    public void whenCreatedFromRequest_build_shouldPopulateDataFromIt() {
        Request request = Request.builder()
                .url("http://valid.url")
                .setBody("abc")
                .setMethod(HttpMethod.DELETE)
                .addHeader("a", "b")
                .build();

        Request newRequest = request.newBuilder().build();

        assertThat(newRequest).isNotSameAs(request);
        assertThat(newRequest.getUrl()).isEqualTo(request.getUrl());
        assertThat(newRequest.getHeaders()).isEqualTo(request.getHeaders());
        assertThat(StreamUtils.readString(newRequest.getBody())).isEqualTo("abc");
        assertThat(newRequest.getMethod()).isEqualTo(request.getMethod());
    }

    static class TestClass {
        @JsonSerialize("a")
        String a;

        TestClass(String a) {
            this.a = a;
        }
    }
}