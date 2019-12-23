package com.jeppeman.globallydynamic.serialization;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jeppeman.globallydynamic.serialization.annotations.JsonDeserialize;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class JsonDeserializerImplTest {
    private JsonDeserializerImpl jsonDeserializerImpl = new JsonDeserializerImpl(); 

    @Test
    public void whenJsonMatchesClass_deserialize_shouldPopulateInstance() {
        String json = "{" +
                "       \"xyz\": 1," +
                "       \"ywf\": {" +
                "           \"abc\": 3" +
                "       }," +
                "       \"k\": \"\"," +
                "       \"z\": null," +
                "       \"array\": [{" +
                "            \"abc\": 10" +
                "        }]," +
                "        \"map\": {" +
                "            \"x\": \"yo\"," +
                "            \"y\": 125.8" +
                "        }," +
                "        i: 5 ," +
                "        'enum': \"TEST1\"," +
                "        set: [[1],[3],[4]]," +
                "        arrayGeneric: [[\"abc\"],[\"def\"]]," +
                "        class: \"java.lang.String\"," +
                "        dontDeserialize: 1," +
                "        enumKeyMap: {" +
                "            TEST1: 1" +
                "        }," +
                "        mapmap: {" +
                "            yolo: {" +
                "                5: [1, 3, 5]" +
                "            }" +
                "        }," +
                "        dontDeserializeThisArray: [{}, {abc: {cde: [{}]}}, 1]" +
                "   }";

        TestClass deserializedObject = jsonDeserializerImpl.deserialize(json, TestClass.class);

        assertThat(deserializedObject.x).isEqualTo(1);
        assertThat(deserializedObject.y.b).isEqualTo(3);
        assertThat(deserializedObject.z).isNull();
        assertThat(deserializedObject.o).isEqualTo("");
        assertThat(deserializedObject.a.get(0).b).isEqualTo(10);
        assertThat(deserializedObject.m.containsKey("x")).isTrue();
        assertThat(deserializedObject.m.containsKey("y")).isTrue();
        assertThat(deserializedObject.m.containsValue("yo")).isTrue();
        assertThat(deserializedObject.m.containsValue("125.8")).isTrue();
        assertThat(deserializedObject.i).isEqualTo(5);
        assertThat(deserializedObject.enumz).isEqualTo(EnumTest.TEST1);
        assertThat(deserializedObject.set).isEqualTo(
                Sets.newHashSet(
                        Sets.newHashSet(1),
                        Sets.newHashSet(3),
                        Sets.newHashSet(4)));
        assertThat(deserializedObject.arrayGeneric).isEqualTo(
                Lists.newArrayList(
                        Lists.newArrayList("abc"),
                        Lists.newArrayList("def")));
        assertThat(deserializedObject.cls).isEqualTo(String.class);
        assertThat(deserializedObject.enumKeyMap.get(EnumTest.TEST1)).isEqualTo(1);
        assertThat(deserializedObject.mapMap.get("yolo").get(5)).isEqualTo(Lists.newArrayList(1, 3, 5));
    }

    @Test
    public void whenRootElementIsArray_deserialize_shouldWork() {
        String json = "[" +
                "       {" +
                "           \"abc\": 1" +
                "       }," +
                "       {" +
                "           \"abc\": 5" +
                "       }" +
                "   ]";

        Object deserializedObject =
            jsonDeserializerImpl.deserialize(json, new JsonDeserializer.TypeWrapper<List<AnotherTestClass>>() {});

        assertThat(deserializedObject).isEqualTo(Arrays.asList(
                new AnotherTestClass(
                        1
                ), new AnotherTestClass(5)
        ));
    }

    @Test(expected = JsonDeserializationException.class)
    public void whenParameterIsNotNullableAndJsonPropertyIsMissing_deserialize_shouldThrowJsonParseException() {
        String json = "{" +
                "       \"xyz\": 1," +
                "       \"k\": null," +
                "       \"array\": [{" +
                "            \"abc\": 10" +
                "        }]," +
                "        \"map\": {" +
                "            \"x\": \"yo\"," +
                "            \"y\": 125.8" +
                "        }" +
                "   }";

        jsonDeserializerImpl.deserialize(json, TestClass.class);
    }

    @Test(expected = JsonDeserializationException.class)
    public void whenParameterTypeIsNotDeserializableFromJson_deserialize_shouldThrowJsonParseException() {
        String json = "{" +
                "       \"xyz\": 1," +
                "       \"k\": null," +
                "       \"array\": [{" +
                "            \"abc\": 10" +
                "        }]," +
                "        \"map\": [{" +
                "            \"x\": \"yo\"," +
                "            \"y\": 125.8" +
                "        }]" +
                "   }";

        jsonDeserializerImpl.deserialize(json, TestClass.class);
    }

    static class AnotherTestClass{
        final int b;
        
        public AnotherTestClass(@JsonDeserialize("abc") int b) {
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof AnotherTestClass && ((AnotherTestClass) o).b == b;
        }
    }

    static class TestClass {
        final int x;
        final AnotherTestClass y;
        final String o;
        final String z;
        final List<AnotherTestClass> a;
        final Map<String, Object> m;
        final int i;
        final EnumTest enumz;
        final Set<Set<Integer>> set;
        final List<List<String>> arrayGeneric;
        final Class<String> cls;
        final Map<EnumTest, Integer> enumKeyMap;
        final Map<String, Map<Integer, List<Integer>>> mapMap;
                
        public TestClass(
                @JsonDeserialize("xyz") int x,
                @JsonDeserialize("ywf") AnotherTestClass y,
                @JsonDeserialize("k") String o,
                @JsonDeserialize("z") String z,
                @JsonDeserialize("array") List<AnotherTestClass> a,
                @JsonDeserialize("map") Map<String, Object> m,
                @JsonDeserialize("i") int i,
                @JsonDeserialize("enum") EnumTest enumz,
                @JsonDeserialize("set") Set<Set<Integer>> set,
                @JsonDeserialize("arrayGeneric") List<List<String>> arrayGeneric,
                @JsonDeserialize("class") Class<String> cls,
                @JsonDeserialize("enumKeyMap") Map<EnumTest, Integer> enumKeyMap,
                @JsonDeserialize("mapmap") Map<String, Map<Integer, List<Integer>>> mapMap) {
            this.x = x;
            this.y = y;
            this.o = o;
            this.z = z;
            this.a = a;
            this.m = m;
            this.i = i;
            this.enumz = enumz;
            this.set = set;
            this.arrayGeneric = arrayGeneric;
            this.cls = cls;
            this.enumKeyMap = enumKeyMap;
            this.mapMap = mapMap;
        }
    }

    enum EnumTest {
        TEST1, TEST2;
    }
}