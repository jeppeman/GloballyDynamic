package com.jeppeman.locallydynamic.serialization;

import com.jeppeman.locallydynamic.serialization.annotations.JsonSerialize;

import org.junit.Test;

import java.util.Arrays;

import static com.google.common.truth.Truth.assertThat;

public class JsonSerializerImplTest {
    private JsonSerializerImpl jsonSerializerImpl = new JsonSerializerImpl();

    @Test
    public void serialize_shouldGenerateValidJsonForAllProperties() {
        String json = jsonSerializerImpl.serialize(new SerializationTestClass());
        char[] actual = json.toCharArray();
        Arrays.sort(actual);
        char[] expected = "{\"x\":1,\"y\":\"hej\",\"z\":[\"a\",1,\"c\",3],\"s\":{\"k\":1},\"enum\":\"TEST1\",\"class\":\"java.lang.String\"}".toCharArray();
        Arrays.sort(expected);

        assertThat(actual).isEqualTo(expected);
    }

    class S {
        @JsonSerialize("k")
        int k = 1;
    }

    class SerializationTestClass {
        @JsonSerialize("x")
        int x = 1;
        @JsonSerialize("y")
        String y = "hej";
        @JsonSerialize("z")
        Object[] z = new Object[]{"a", 1, "c", 3};
        @JsonSerialize("s")
        S s = new S();
        @JsonSerialize("enum")
        TestEnum enumz = TestEnum.TEST1;
        @JsonSerialize("class")
        Class<String> cls = String.class;
    }

    enum TestEnum {
        TEST1
    }
}