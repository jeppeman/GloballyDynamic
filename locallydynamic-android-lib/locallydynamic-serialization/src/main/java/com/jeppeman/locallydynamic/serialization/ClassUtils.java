package com.jeppeman.locallydynamic.serialization;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class ClassUtils {
    static List<Field> getAllFields(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        List<Field> fields = new LinkedList<Field>();
        fields.addAll(Arrays.asList(declaredFields));
        if (clazz.getSuperclass() != null) {
            fields.addAll(getAllFields(clazz.getSuperclass()));
        }

        return fields;
    }

    static List<Method> getAllMethods(Class<?> clazz) {
        Method[] declaredMethods = clazz.getDeclaredMethods();
        List<Method> methods = new LinkedList<Method>();
        methods.addAll(Arrays.asList(declaredMethods));
        if (clazz.getSuperclass() != null) {
            methods.addAll(getAllMethods(clazz.getSuperclass()));
        }

        return methods;
    }

    static Field findField(Class<?> clazz, String name) {
        List<Field> allFields = getAllFields(clazz);

        for (Field field : allFields) {
            if (name.equals(field.getName())) {
                return field;
            }
        }

        return null;
    }

    static Type[] getGenericTypeArguments(Type type) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getActualTypeArguments();
        }

        return new Type[0];
    }

    static boolean isString(Class<?> clazz) {
        return String.class.isAssignableFrom(clazz);
    }

    static boolean isBigDecimal(Class<?> clazz) {
        return BigDecimal.class.isAssignableFrom(clazz);
    }

    static boolean isBoolean(Class<?> clazz) {
        return Boolean.class.isAssignableFrom(clazz)
                || boolean.class.isAssignableFrom(clazz);
    }

    static boolean isInt(Class<?> clazz) {
        return Integer.class.isAssignableFrom(clazz)
                || int.class.isAssignableFrom(clazz);
    }

    static boolean isLong(Class<?> clazz) {
        return Long.class.isAssignableFrom(clazz)
                || long.class.isAssignableFrom(clazz);
    }

    static boolean isShort(Class<?> clazz) {
        return Short.class.isAssignableFrom(clazz)
                || short.class.isAssignableFrom(clazz);
    }

    static boolean isDouble(Class<?> clazz) {
        return Double.class.isAssignableFrom(clazz)
                || double.class.isAssignableFrom(clazz);
    }

    static boolean isFloat(Class<?> clazz) {
        return Float.class.isAssignableFrom(clazz)
                || float.class.isAssignableFrom(clazz);
    }

    static boolean isList(Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    static boolean isLinkedList(Class<?> clazz) {
        return LinkedList.class.isAssignableFrom(clazz);
    }

    static boolean isArrayList(Class<?> clazz) {
        return ArrayList.class.isAssignableFrom(clazz);
    }

    static boolean isSet(Class<?> clazz) {
        return Set.class.isAssignableFrom(clazz);
    }

    static boolean isHashSet(Class<?> clazz) {
        return HashSet.class.isAssignableFrom(clazz);
    }

    static boolean isLinkedHashSet(Class<?> clazz) {
        return LinkedHashSet.class.isAssignableFrom(clazz);
    }

    static boolean isCollection(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    static boolean isIterable(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    static boolean isMap(Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    static boolean isHashMap(Class<?> clazz) {
        return HashMap.class.isAssignableFrom(clazz);
    }

    static boolean isTreeMap(Class<?> clazz) {
        return TreeMap.class.isAssignableFrom(clazz);
    }
}