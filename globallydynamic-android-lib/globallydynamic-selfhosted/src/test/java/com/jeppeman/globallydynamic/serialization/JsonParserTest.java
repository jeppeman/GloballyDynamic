package com.jeppeman.globallydynamic.serialization;

import com.google.common.collect.Lists;

import org.junit.Test;

import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;

public class JsonParserTest {

    private JsonParser jsonParser = new JsonParser();

    @Test
    public void whenJsonIsValid_parse_shouldReportEventsToSubscriber() {
        String json = "{" +
                "       \"xyz\": 1," +
                "       \"ywf\": {" +
                "           \"abc\": 3" +
                "       }," +
                "       \"k\": null," +
                "       \"ar{ray\": [{" +
                "            \"abc\": 10" +
                "        }]," +
                "        \"map\": {" +
                "            \"x\": \"yo\"," +
                "            \"y\": 125.8" +
                "        }," +
                "        i: 5," +
                "        a: false," +
                "        b: true," +
                "        r: \"x\\\"yx\\\"\"" +
                "   }";
        final int[] nBeginObject = new int[] {0};
        final int[] nEndObject = new int[] {0};
        final int[] nBeginArray = new int[] {0};
        final int[] nEndArray = new int[] {0};
        final ArrayList<String> keys = new ArrayList<String>();
        final ArrayList<Object> primitives = new ArrayList<Object>();

        jsonParser.parse(json, new JsonParser.Callbacks() {
            public void keyParsed(String key) {
                keys.add(key);
            }

            public void primitiveParsed(String value) {
                primitives.add(value);
            }

            public void beginObject() {
                nBeginObject[0]++;
            }

            public void endObject() {
                nEndObject[0]++;
            }

            public void beginArray() {
                nBeginArray[0]++;
            }

            public void endArray() {
                nEndArray[0]++;
            }
        });

        assertThat(nBeginObject[0]).isEqualTo(4);
        assertThat(nEndObject[0]).isEqualTo(4);
        assertThat(nBeginArray[0]).isEqualTo(1);
        assertThat(nEndArray[0]).isEqualTo(1);
        assertThat(keys).isEqualTo(
                Lists.newArrayList(
                        "xyz",
                        "ywf",
                        "abc",
                        "k",
                        "ar{ray",
                        "abc",
                        "map",
                        "x",
                        "y",
                        "i",
                        "a",
                        "b",
                        "r"
                )
        );
        assertThat(primitives).isEqualTo(
            Lists.newArrayList(
                "1",
                "3",
                "null",
                "10",
                "yo",
                "125.8",
                "5",
                "false",
                "true",
                "x\\\"yx\\\""
            )
        );
    }

    @Test(expected = JsonParseException.class)
    public void whenJsonIsNotValid_parse_shouldThrowJsonParseException() {
        String json = "{" +
                "       \"xyz\": 1," +
                "       \"ywf\": {" +
                "           \"abc\": 3" +
                "       }," +
                "       \"k\": null," +
                "       \"array\": [{" +
                "            \"abc\": 10" +
                "        }]," +
                "        \"map\": {" +
                "            \"x\": \"yo\"," +
                "            \"y\": 125.8" +
                "        }" +
                "        i: 5" +
                "   }";

        jsonParser.parse(json, new JsonParser.Callbacks() {
            public void keyParsed(String key) {}
            public void primitiveParsed(String value) {}
            public void beginObject() {}
            public void endObject() {}
            public void beginArray() {}
            public void endArray() {}
        });
    }
}