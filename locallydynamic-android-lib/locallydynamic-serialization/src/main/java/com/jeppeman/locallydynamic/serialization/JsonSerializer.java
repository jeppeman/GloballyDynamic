package com.jeppeman.locallydynamic.serialization;

import com.jeppeman.locallydynamic.serialization.annotations.JsonSerialize;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public interface JsonSerializer {
    String serialize(Object obj);
}

class JsonSerializerImpl implements JsonSerializer {
    private JsonElement toJson(Object obj) {
        try {
            if (obj == null) {
                return new JsonNull();
            }
            Class<?> type = obj.getClass();

            JsonElement element;
            if (obj instanceof Class<?>) {
                element = new JsonPrimitive(((Class) obj).getName());
            } else if (ClassUtils.isBigDecimal(type)) {
                element = new JsonPrimitive((BigDecimal) obj);
            } else if (type.isEnum()) {
                element = new JsonPrimitive(((Enum<?>) obj).name());
            } else if (ClassUtils.isBoolean(type)) {
                element = new JsonPrimitive((Boolean) obj);
            } else if (ClassUtils.isInt(type)) {
                element = new JsonPrimitive((Integer) obj);
            } else if (ClassUtils.isLong(type)) {
                element = new JsonPrimitive((Long) obj);
            } else if (ClassUtils.isShort(type)) {
                element = new JsonPrimitive((Short) obj);
            } else if (ClassUtils.isFloat(type)) {
                element = new JsonPrimitive((Float) obj);
            } else if (ClassUtils.isDouble(type)) {
                element = new JsonPrimitive((Double) obj);
            } else if (ClassUtils.isString(type)) {
                element = new JsonPrimitive((String) obj);
            } else if (ClassUtils.isIterable(type)) {
                JsonArray jsonArray = new JsonArray();
                Iterable iterable = (Iterable<?>) obj;
                for (Object item : iterable) {
                    jsonArray.push(toJson(item));
                }
                element = jsonArray;
            } else if (type.isArray()) {
                JsonArray jsonArray = new JsonArray();
                Object[] iterable = (Object[]) obj;
                for (Object item : iterable) {
                    jsonArray.push(toJson(item));
                }
                element = jsonArray;
            } else if (ClassUtils.isMap(type)) {
                JsonObject jsonObject = new JsonObject();

                Map<?, ?> map = (Map<?, ?>) obj;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    jsonObject.put(entry.getKey().toString(), toJson(entry.getValue()));
                }

                element = jsonObject;
            } else {
                JsonObject jsonObject = new JsonObject();
                List<Field> allFields = ClassUtils.getAllFields(type);

                for (Field field : allFields) {
                    if (field.isAnnotationPresent(JsonSerialize.class)) {
                        JsonSerialize annotation = field.getAnnotation(JsonSerialize.class);
                        boolean wasAccessible = field.isAccessible();
                        field.setAccessible(true);
                        jsonObject.put(annotation.value(), toJson(field.get(obj)));
                        field.setAccessible(wasAccessible);
                    }
                }

                element = jsonObject;
            }

            return element;

        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public String serialize(Object obj) {
        return toJson(obj).toString();
    }
}