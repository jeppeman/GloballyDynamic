package com.jeppeman.globallydynamic.globalsplitinstall;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

class ReflectionUtils {
    static List<Field> getAllFields(Class<?> cls) {
        List<Field> ret = new LinkedList<Field>();
        while (cls != null) {
            ret.addAll(Arrays.asList(cls.getDeclaredFields()));
            cls = cls.getSuperclass();
        }

        return ret;
    }

    static List<Method> getAllMethods(Class<?> cls) {
        List<Method> ret = new LinkedList<Method>();
        while (cls != null) {
            ret.addAll(Arrays.asList(cls.getDeclaredMethods()));
            cls = cls.getSuperclass();
        }

        return ret;
    }

    static Method getMethod(Object target, String name, Class<?>... params) throws NoSuchMethodException {
        List<Method> allMethods = getAllMethods(target.getClass());
        for (Method method : allMethods) {
            if (name.equals(method.getName())) {
                boolean parametersMatch = params.length == 0;
                for (int i = 0; i < params.length; i++) {
                    Class<?> paramType = method.getParameterTypes()[i];
                    if (paramType.equals(params[i])) {
                        parametersMatch = true;
                        break;
                    }
                }

                if (parametersMatch) {
                    return method;
                }
            }
        }

        throw new NoSuchMethodException(name);
    }

    static Field getField(Object target, String name) throws NoSuchFieldException {
        return getField(target.getClass(), name);
    }

    static Field getField(Class<?> cls, String name) throws NoSuchFieldException {
        List<Field> allFields = getAllFields(cls);
        for (Field field : allFields) {
            if (name.equals(field.getName())) {
                return field;
            }
        }

        throw new NoSuchFieldException(name);
    }

    static Object getFieldValue(Object target, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field = getField(target, name);
        boolean wasAccessible = field.isAccessible();
        field.setAccessible(true);
        Object ret = field.get(target);
        field.setAccessible(wasAccessible);
        return ret;

    }

    static Object getFieldValue(Class<?> cls, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field = getField(cls, name);
        boolean wasAccessible = field.isAccessible();
        field.setAccessible(true);
        Object ret = field.get(null);
        field.setAccessible(wasAccessible);
        return ret;
    }

    static void setFieldValue(Object target, String name, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = getField(target, name);
        boolean wasAccessible = field.isAccessible();
        field.setAccessible(true);
        field.set(target, value);
        field.setAccessible(wasAccessible);
    }

    static Object invokeMethod(Object target, String name, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = getMethod(target, name);
        boolean wasAccessible = method.isAccessible();
        method.setAccessible(true);
        Object ret = method.invoke(target, args);
        method.setAccessible(wasAccessible);
        return ret;
    }
}
