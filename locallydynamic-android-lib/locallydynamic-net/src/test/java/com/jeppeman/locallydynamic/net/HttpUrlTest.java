package com.jeppeman.locallydynamic.net;

import com.google.common.collect.Lists;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class HttpUrlTest {
    @Test
    public void whenUrlIsValidWithQueryParams_parse_shouldGenerateValidUrl() {
        String url = "https://this.is.a.valid.url:443/one/segment/and/more/segments?x=y&o=p";

        HttpUrl parsedUrl = HttpUrl.parse(url);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("x", "y");
        map.put("o", "p");
        assertThat(parsedUrl.port()).isEqualTo(443);
        assertThat(parsedUrl.host()).isEqualTo("this.is.a.valid.url");
        assertThat(parsedUrl.scheme()).isEqualTo("https");
        assertThat(parsedUrl.pathSegments()).isEqualTo(Lists.newArrayList("one", "segment", "and", "more", "segments"));
        assertThat(parsedUrl.queryParams()).isEqualTo(map);
        assertThat(parsedUrl.url()).isEqualTo(url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSchemeIsNotHttpOrHttps_parse_shouldThrow() {
        String url = "htt://this.is.a.valid.url/one/segment/and/more/segments?x=y&o=p";

        HttpUrl.parse(url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSchemeContainsNoColon_parse_shouldThrow() {
        String url = "https//this.is.a.valid.url/one/segment/and/more/segments?x=y&o=p";

        HttpUrl.parse(url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSchemeContainsOnlyOneSlash_parse_shouldThrow() {
        String url = "https:/this.is.a.valid.url/one/segment/and/more/segments?x=y&o=p";

        HttpUrl.parse(url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSchemeContainsMoreThanTwoSlashes_parse_shouldThrow() {
        String url = "https:///this.is.a.valid.url/one/segment/and/more/segments?x=y&o=p";

        HttpUrl.parse(url);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenQueryParameterIsInvalid_parse_shouldThrow() {
        String url = "https:///this.is.a.valid.url/one/segment/and/more/segments??x=y&o=p";

        HttpUrl.parse(url);
    }
}