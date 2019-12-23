package com.jeppeman.locallydynamic.serialization;

import com.jeppeman.locallydynamic.serialization.annotations.JsonDeserialize;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

public interface JsonDeserializer {
    <T> T deserialize(String json, Class<T> type);

    <T> T deserialize(String json, TypeWrapper<T> typeWrapper);

    abstract class TypeWrapper<T> {
        private final Class<?> rawType;
        private final Type[] genericTypeArguments;

        {
            TypeInfo typeInfo = getTypeInfo(
                    ClassUtils.getGenericTypeArguments(getClass().getGenericSuperclass())[0]
            );
            this.rawType = typeInfo.rawType;
            this.genericTypeArguments = typeInfo.genericTypeArguments;
        }

        private TypeInfo getTypeInfo(Type type) {
            Type rawValueType;
            Type[] valueGenericTypeArgs;
            if (type instanceof WildcardType) {
                Type bound = ((WildcardType) type).getUpperBounds().length > 0
                        ? ((WildcardType) type).getUpperBounds()[0]
                        : ((WildcardType) type).getLowerBounds()[0];
                if (bound instanceof ParameterizedType) {
                    rawValueType = ((ParameterizedType) bound).getRawType();
                    valueGenericTypeArgs = ((ParameterizedType) bound).getActualTypeArguments();
                } else {
                    rawValueType = bound;
                    valueGenericTypeArgs = new Type[0];
                }
            } else if (type instanceof ParameterizedType) {
                rawValueType = ((ParameterizedType) type).getRawType();
                valueGenericTypeArgs = ((ParameterizedType) type).getActualTypeArguments();
            } else {
                rawValueType = type;
                valueGenericTypeArgs = new Type[0];
            }

            return TypeInfo.of((Class<?>) rawValueType, valueGenericTypeArgs);
        }

        public Class<?> getRawType() {
            return rawType;
        }

        public Type[] getGenericTypeArguments() {
            return genericTypeArguments;
        }
    }
}

class JsonDeserializerImpl implements JsonDeserializer {
    private final JsonParser parser = new JsonParser();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(String json, JsonDeserializer.TypeWrapper<T> typeWrapper) {
        JsonDeserializerContext jsonDeserializerContext =
                new JsonDeserializerContext(typeWrapper.getRawType(), typeWrapper.getGenericTypeArguments());
        parser.parse(json, jsonDeserializerContext);
        return (T) jsonDeserializerContext.getDeserializedObject();
    }

    @Override
    public <T> T deserialize(String json, Class<T> type) {
        JsonDeserializerContext jsonDeserializerContext = new JsonDeserializerContext(type);
        parser.parse(json, jsonDeserializerContext);
        return type.cast(jsonDeserializerContext.getDeserializedObject());
    }
}

class JsonDeserializerContext implements JsonParser.Callbacks {
    private final Stack<DeserializationTypeInfo> types = new Stack<DeserializationTypeInfo>();
    private final Stack<State> states = new Stack<State>();
    private final Stack<KeyInfo> parsedKeys = new Stack<KeyInfo>();
    private final Map<Class<?>, ConstructorInfo> constructorCache = new HashMap<Class<?>, ConstructorInfo>();
    private Object deserializedObject = null;

    JsonDeserializerContext(
            Class<?> rootType,
            Type... genericTypeArgs) {
        pushNext(rootType, genericTypeArgs);
        states.push(Initial.INSTANCE);
    }


    private Class<?> getCurrentType() {
        return types.peek().rawType;
    }

    private State getCurrentState() {
        return states.peek();
    }

    private KeyInfo getCurrentKey() {
        return parsedKeys.size() > 0
                ? parsedKeys.lastElement()
                : KeyInfo.of("", true);
    }

    private ConstructorInfo getConstructorInfoForType(Class<?> type) {
        if (constructorCache.containsKey(type)) {
            return constructorCache.get(type);
        } else {
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            Constructor<?> resolvedCtor = null;
            for (Constructor<?> constructor : constructors) {
                for (Annotation[] annotations : constructor.getParameterAnnotations()) {
                    for (Annotation annotation : annotations) {
                        if (annotation instanceof JsonDeserialize) {
                            resolvedCtor = constructor;
                            break;
                        }
                    }
                }
            }

            if (resolvedCtor != null) {
                List<Annotation> parameterAnnotations = new LinkedList<Annotation>();
                for (Annotation[] annotations : resolvedCtor.getParameterAnnotations()) {
                    parameterAnnotations.addAll(Arrays.asList(annotations));
                }
                ConstructorInfo constructorInfo = new ConstructorInfo(
                        resolvedCtor,
                        resolvedCtor.getParameterTypes(),
                        resolvedCtor.getGenericParameterTypes(),
                        parameterAnnotations
                );

                constructorCache.put(type, constructorInfo);

                return constructorInfo;
            } else {
                return null;
            }
        }
    }

    private boolean deserializableFromJsonArray(Class<?> javaType) {
        return javaType.isArray()
                || ClassUtils.isSet(javaType)
                || ClassUtils.isList(javaType)
                || ClassUtils.isCollection(javaType)
                || ClassUtils.isIterable(javaType);
    }

    private boolean deserializableFromJsonPrimitive(Class<?> javaType) {
        return ClassUtils.isBigDecimal(javaType)
                || ClassUtils.isBoolean(javaType)
                || ClassUtils.isInt(javaType)
                || ClassUtils.isShort(javaType)
                || ClassUtils.isFloat(javaType)
                || ClassUtils.isDouble(javaType)
                || ClassUtils.isString(javaType)
                || javaType.isEnum()
                || javaType == Class.class;
    }

    private boolean shouldDeserializeKey(String key, Class<?> javaType) {
        boolean hasDeserializeAnnotationForKey = false;
        ConstructorInfo constructorInfo = getConstructorInfoForType(javaType);
        if (constructorInfo != null) {
            for (Annotation annotation : constructorInfo.parameterAnnotations) {
                if (annotation instanceof JsonDeserialize
                        && key.equals(((JsonDeserialize) annotation).value())) {
                    hasDeserializeAnnotationForKey = true;
                    break;
                }
            }
        }

        return ClassUtils.isMap(javaType) || hasDeserializeAnnotationForKey;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object deserializeJsonPrimitive(String value, Class<?> javaType) {
        try {
            Object deserialized;
            if ("null".equals(value)) {
                deserialized = null;
            } else if (ClassUtils.isBigDecimal(javaType)) {
                deserialized = new BigDecimal(value);
            } else if (ClassUtils.isBoolean(javaType)) {
                deserialized = Boolean.parseBoolean(value);
            } else if (ClassUtils.isInt(javaType)) {
                deserialized = Integer.parseInt(value);
            } else if (ClassUtils.isLong(javaType)) {
                deserialized = Long.parseLong(value);
            } else if (ClassUtils.isShort(javaType)) {
                deserialized = Short.parseShort(value);
            } else if (ClassUtils.isFloat(javaType)) {
                deserialized = Float.parseFloat(value);
            } else if (ClassUtils.isDouble(javaType)) {
                deserialized = Double.parseDouble(value);
            } else if (javaType.isEnum()) {
                deserialized = javaType.cast(Enum.valueOf((Class<? extends Enum>) javaType, value));
            } else if (javaType == Class.class) {
                deserialized = Class.forName(value);
            } else {
                deserialized = value;
            }

            return deserialized;
        } catch (Exception exception) {
            throw new JsonDeserializationException(
                    "Failed to deserialize " + value + " to " + javaType,
                    exception
            );
        }
    }

    private TypeInfo getTypeInfo(Type type) {
        Type rawValueType;
        Type[] valueGenericTypeArgs;
        if (type instanceof WildcardType) {
            Type bound = ((WildcardType) type).getUpperBounds().length > 0
                    ? ((WildcardType) type).getUpperBounds()[0]
                    : ((WildcardType) type).getLowerBounds()[0];
            if (bound instanceof ParameterizedType) {
                rawValueType = ((ParameterizedType) bound).getRawType();
                valueGenericTypeArgs = ((ParameterizedType) bound).getActualTypeArguments();
            } else {
                rawValueType = bound;
                valueGenericTypeArgs = new Type[0];
            }
        } else if (type instanceof ParameterizedType) {
            rawValueType = ((ParameterizedType) type).getRawType();
            valueGenericTypeArgs = ((ParameterizedType) type).getActualTypeArguments();
        } else {
            rawValueType = type;
            valueGenericTypeArgs = new Type[0];
        }

        return TypeInfo.of((Class<?>) rawValueType, valueGenericTypeArgs);
    }

    private Object deserializeJsonArray(Class<?> javaType, List<Object> children) {
        if (javaType.isArray()) {
            Object[] array = (Object[]) Array.newInstance(
                    javaType.getComponentType(),
                    children.size()
            );

            for (int i = 0; i < children.size(); i++) {
                Object arrayVal = children.get(i);
                array[i] = arrayVal;
            }

            return array;
        } else if (ClassUtils.isSet(javaType)) {
            Set<?> set;
            if (ClassUtils.isLinkedHashSet(javaType)) {
                set = new LinkedHashSet<Object>(children);
            } else if (ClassUtils.isHashSet(javaType)) {
                set = new HashSet<Object>(children);
            } else {
                set = new HashSet<Object>(children);
            }

            return set;
        } else if (ClassUtils.isList(javaType)) {
            List list;

            if (ClassUtils.isLinkedList(javaType)) {
                list = new LinkedList<Object>(children);
            } else if (ClassUtils.isArrayList(javaType)) {
                list = new ArrayList<Object>(children);
            } else {
                list = new ArrayList<Object>(children);
            }

            return list;
        } else if (ClassUtils.isCollection(javaType) || ClassUtils.isIterable(javaType)) {
            return new ArrayList<Object>(children);
        } else {
            throw new JsonDeserializationException(javaType + " is not deserializable from json array");
        }
    }

    private Object deserializeJsonObject(
            Class<?> javaType,
            List<DeserializationKeyValuePair> deserializationKeyValuePairs,
            Type... genericTypeArgs) {
        if (ClassUtils.isMap(javaType)) {
            Map<Object, Object> entries = new LinkedHashMap<Object, Object>();
            for (DeserializationKeyValuePair deserializationKeyValuePair : deserializationKeyValuePairs) {
                Type keyType = genericTypeArgs.length > 0
                        ? genericTypeArgs[0]
                        : Object.class;
                Class<?> rawKeyType = getTypeInfo(keyType).rawType;
                Object deserializedKey = deserializeJsonPrimitive(
                        deserializationKeyValuePair.key, rawKeyType);
                entries.put(deserializedKey, deserializationKeyValuePair.value);
            }

            Map map;
            if (ClassUtils.isTreeMap(javaType)) {
                map = new TreeMap<Object, Object>(entries);
            } else if (ClassUtils.isHashMap(javaType)) {
                map = new HashMap<Object, Object>(entries);
            } else {
                map = new HashMap<Object, Object>(entries);
            }

            return map;
        } else {
            ConstructorInfo constructorInfo = getConstructorInfoForType(javaType);
            List<Object> constructorArgs = new LinkedList<Object>();
            if (constructorInfo != null) {
                for (Annotation annotation : constructorInfo.parameterAnnotations) {
                    if (annotation instanceof JsonDeserialize) {
                        deserializaitonKeyValuePairs:
                        for (DeserializationKeyValuePair deserializationKeyValuePair
                                : deserializationKeyValuePairs) {
                            if (deserializationKeyValuePair.key.equals(
                                    ((JsonDeserialize) annotation).value())) {
                                constructorArgs.add(deserializationKeyValuePair.value);
                                break deserializaitonKeyValuePairs;
                            }
                        }
                    }
                }
            }

            if (constructorInfo == null
                    || constructorInfo.constructor == null
                    || (constructorArgs.size() != constructorInfo.constructor.getParameterTypes().length)) {
                throw new JsonDeserializationException(
                        "No suitable constructor was found for deserialization of class " + javaType +
                                ", make sure all constructor parameters are annotated with @JsonDeserialize and that" +
                                " @JsonDeserialize#value() matches its corresponding json property."
                );
            }

            try {
                Constructor<?> ctor = constructorInfo.constructor;
                boolean wasAccessible = ctor.isAccessible();
                ctor.setAccessible(true);
                Object instance = constructorInfo.constructor.newInstance(constructorArgs.toArray());
                ctor.setAccessible(wasAccessible);
                return instance;
            } catch (Exception exception) {
                throw new JsonDeserializationException(
                        "Failed to create instance of " + javaType + "from",
                        exception
                );
            }
        }
    }

    private void pushNext(Class<?> type, Type... genericTypeArgs) {
        types.push(DeserializationTypeInfo.of(type, genericTypeArgs, new LinkedList<Object>()));
    }

    private void addToCurrent(Object deserializedObject) {
        if (!types.isEmpty()) {
            types.peek().constructorArgs.add(deserializedObject);
        } else {
            this.deserializedObject = deserializedObject;
        }
    }

    private void finalizeCurrent(Object deserializedObject) {
        boolean shouldDeserializeCurrent = getCurrentKey().shouldDeserialize;
        if (shouldDeserializeCurrent) {
            types.pop();
        }
        states.pop();
        getCurrentState().objectDeserialized(this, deserializedObject);
    }

    Object getDeserializedObject() {
        return deserializedObject;
    }

    @Override
    public void keyParsed(String key) {
        getCurrentState().keyParsed(this, key);
    }

    @Override
    public void primitiveParsed(String value) {
        getCurrentState().primitiveParsed(this, value);
    }

    @Override
    public void beginObject() {
        getCurrentState().beginObject(this);
    }

    @Override
    public void endObject() {
        getCurrentState().endObject(this);
    }

    @Override
    public void beginArray() {
        getCurrentState().beginArray(this);
    }

    @Override
    public void endArray() {
        getCurrentState().endArray(this);
    }

    static abstract class State {
        void keyParsed(JsonDeserializerContext context, String key) {
        }

        void primitiveParsed(JsonDeserializerContext context, String value) {
        }

        void beginObject(JsonDeserializerContext context) {
            boolean shouldDeserializeCurrent = context.getCurrentKey().shouldDeserialize;
            if (shouldDeserializeCurrent
                    && (context.deserializableFromJsonPrimitive(context.getCurrentType())
                    || context.deserializableFromJsonArray(context.getCurrentType()))
            ) {
                throw new JsonDeserializationException("Unexpected beginObject for type " + context.getCurrentType());
            }
            context.states.push(InObject.INSTANCE);
        }

        void endObject(JsonDeserializerContext context) {
        }

        void beginArray(JsonDeserializerContext context) {
            boolean shouldDeserializeCurrent = context.getCurrentKey().shouldDeserialize;
            if (shouldDeserializeCurrent && !context.deserializableFromJsonArray(context.getCurrentType())) {
                throw new JsonDeserializationException("Unexpected beginArray for type " + context.getCurrentType());
            }
            context.states.push(InArray.INSTANCE);
        }

        void endArray(JsonDeserializerContext context) {
        }

        void objectDeserialized(JsonDeserializerContext context, Object deserializedObject) {
            context.addToCurrent(deserializedObject);
        }
    }

    static class Initial extends State {
        static final Initial INSTANCE = new Initial();

        private Initial() {
        }

        @Override
        public void primitiveParsed(JsonDeserializerContext context, String value) {
            context.deserializeJsonPrimitive(value, context.getCurrentType());
        }
    }

    static class InArray extends State {
        static final InArray INSTANCE = new InArray();

        private InArray() {
        }

        private Type getComponentTypeOfCurrent(JsonDeserializerContext context) {
            Type componentType;
            if (context.getCurrentType().isArray()) {
                componentType = context.getCurrentType().getComponentType();
            } else {
                Type[] types = context.types.peek().genericTypeArguments;
                componentType = types.length > 0
                        ? types[0]
                        : Class.class;
            }

            return componentType;
        }

        @Override
        public void beginObject(JsonDeserializerContext context) {
            boolean shouldDeserializeCurrent = context.getCurrentKey().shouldDeserialize;
            if (shouldDeserializeCurrent) {
                TypeInfo typeInfo = context.getTypeInfo(
                        getComponentTypeOfCurrent(
                                context
                        )
                );

                context.pushNext(typeInfo.rawType, typeInfo.genericTypeArguments);
            }

            super.beginObject(context);
        }

        @Override
        public void beginArray(JsonDeserializerContext context) {
            boolean shouldDeserializeCurrent = context.getCurrentKey().shouldDeserialize;
            if (shouldDeserializeCurrent) {
                Type componentType = getComponentTypeOfCurrent(context);
                TypeInfo typeInfo = context.getTypeInfo(componentType);
                context.pushNext(typeInfo.rawType, typeInfo.genericTypeArguments);
            }

            super.beginArray(context);
        }

        @Override
        public void endArray(JsonDeserializerContext context) {
            boolean shouldDeserializeCurrent = context.getCurrentKey().shouldDeserialize;
            if (shouldDeserializeCurrent) {
                DeserializationTypeInfo deserializationTypeInfo = context.types.peek();
                Object finalizedArray = context.deserializeJsonArray(
                        deserializationTypeInfo.rawType,
                        deserializationTypeInfo.constructorArgs);
                context.finalizeCurrent(finalizedArray);
            } else {
                context.finalizeCurrent(null);
            }
        }

        @Override
        public void primitiveParsed(JsonDeserializerContext context, String value) {
            boolean shouldDeserializeCurrent = context.getCurrentKey().shouldDeserialize;
            if (shouldDeserializeCurrent) {
                Type componentType = getComponentTypeOfCurrent(context);
                TypeInfo typeInfo = context.getTypeInfo(componentType);
                Object deserializedPrimitive = context.deserializeJsonPrimitive(
                        value,
                        typeInfo.rawType);
                context.addToCurrent(deserializedPrimitive);
            }
        }
    }

    static class InObject extends State {
        static final InObject INSTANCE = new InObject();

        private InObject() {
        }

        private void pushNextType(JsonDeserializerContext context, String key) {
            TypeInfo typeInfo = getTypeInfoOfKey(context, key);
            context.pushNext(typeInfo.rawType, typeInfo.genericTypeArguments);
        }

        private TypeInfo getTypeInfoOfKey(
                JsonDeserializerContext context,
                String key
        ) {
            TypeInfo typeInfo;
            if (ClassUtils.isMap(context.getCurrentType())) {
                Type[] types = context.types.peek().genericTypeArguments;
                Type type = types.length > 1
                        ? types[1]
                        : Class.class;
                typeInfo = context.getTypeInfo(type);
            } else {
                ConstructorInfo constructorInfo = context.getConstructorInfoForType(context.getCurrentType());
                Integer parameterIndex = null;
                if (constructorInfo != null) {
                    int i = 0;
                    for (Annotation annotation : constructorInfo.parameterAnnotations) {
                        if (annotation instanceof JsonDeserialize
                                && ((JsonDeserialize) annotation).value().equals(key)) {
                            parameterIndex = i;
                        }
                        i++;
                    }
                }

                if (parameterIndex == null) {
                    throw new JsonDeserializationException("Type " + context.getCurrentType() + "has no @JsonDeserialize with value " + key);
                }

                Class<?> typeOfKey = constructorInfo.parameterTypes[parameterIndex];
                typeInfo = TypeInfo.of(typeOfKey,
                        ClassUtils.getGenericTypeArguments(
                                constructorInfo.genericParameterTypes[parameterIndex]));
            }

            return typeInfo;
        }

        @Override
        public void beginObject(JsonDeserializerContext context) {
            KeyInfo keyInfo = context.getCurrentKey();
            if (keyInfo.shouldDeserialize) {
                pushNextType(context, keyInfo.key);
            }
            super.beginObject(context);
        }

        @Override
        public void beginArray(JsonDeserializerContext context) {
            KeyInfo keyInfo = context.getCurrentKey();
            if (keyInfo.shouldDeserialize) {
                pushNextType(context, keyInfo.key);
            }
            super.beginArray(context);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void endObject(JsonDeserializerContext context) {
            boolean shouldDeserializeCurrent = context.getCurrentKey().shouldDeserialize;
            if (shouldDeserializeCurrent) {
                DeserializationTypeInfo deserializationTypeInfo = context.types.peek();
                List<DeserializationKeyValuePair> constructorArgs =
                        new LinkedList<DeserializationKeyValuePair>();
                for (Object child : deserializationTypeInfo.constructorArgs) {
                    if (child instanceof DeserializationKeyValuePair) {
                        constructorArgs.add((DeserializationKeyValuePair) child);
                    }
                }
                Object finalizedObject = context.deserializeJsonObject(
                        deserializationTypeInfo.rawType,
                        constructorArgs,
                        deserializationTypeInfo.genericTypeArguments
                );
                context.finalizeCurrent(finalizedObject);
            } else {
                context.finalizeCurrent(null);
            }
        }

        @Override
        public void keyParsed(JsonDeserializerContext context, String key) {
            boolean shouldDeserialize;
            if (!context.parsedKeys.isEmpty()) {
                boolean shouldDeserializePrevious = context.parsedKeys.peek().shouldDeserialize;
                if (!shouldDeserializePrevious) {
                    shouldDeserialize = false;
                } else {
                    shouldDeserialize = context.shouldDeserializeKey(
                            key,
                            context.getCurrentType()
                    );
                }
            } else {
                shouldDeserialize = context.shouldDeserializeKey(key, context.getCurrentType());
            }
            context.parsedKeys.push(KeyInfo.of(key, shouldDeserialize));
        }

        @Override
        public void primitiveParsed(JsonDeserializerContext context, String value) {
            KeyInfo keyInfo = context.parsedKeys.pop();
            if (keyInfo.shouldDeserialize) {
                Object deserializedValue =
                        context.deserializeJsonPrimitive(
                                value, getTypeInfoOfKey(context, keyInfo.key).rawType);
                context.addToCurrent(DeserializationKeyValuePair.of(keyInfo.key, deserializedValue));
            }
        }

        @Override
        public void objectDeserialized(JsonDeserializerContext context, Object deserializedObject) {
            KeyInfo keyInfo = context.parsedKeys.pop();
            if (keyInfo.shouldDeserialize) {
                context.addToCurrent(DeserializationKeyValuePair.of(keyInfo.key, deserializedObject));
            }
        }
    }
}

class DeserializationKeyValuePair {
    final String key;
    final Object value;

    private DeserializationKeyValuePair(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    static DeserializationKeyValuePair of(String key, Object value) {
        return new DeserializationKeyValuePair(key, value);
    }
}

class KeyInfo {
    final String key;
    final boolean shouldDeserialize;

    private KeyInfo(String key, boolean shouldDeserialize) {
        this.key = key;
        this.shouldDeserialize = shouldDeserialize;
    }

    static KeyInfo of(String key, boolean shouldDeserialize) {
        return new KeyInfo(key, shouldDeserialize);
    }
}

class TypeInfo {
    final Class<?> rawType;
    final Type[] genericTypeArguments;

    protected TypeInfo(Class<?> rawType, Type[] genericTypeArguments) {
        this.rawType = rawType;
        this.genericTypeArguments = genericTypeArguments;
    }

    static TypeInfo of(Class<?> rawType, Type[] genericTypeArguments) {
        return new TypeInfo(rawType, genericTypeArguments);
    }
}

class DeserializationTypeInfo extends TypeInfo {
    final List<Object> constructorArgs;

    private DeserializationTypeInfo(
            Class<?> rawType,
            Type[] genericTypeArguments,
            List<Object> constructorArgs) {
        super(rawType, genericTypeArguments);
        this.constructorArgs = constructorArgs;
    }

    static DeserializationTypeInfo of(
            Class<?> rawType,
            Type[] genericTypeArguments,
            List<Object> constructorArgs) {
        return new DeserializationTypeInfo(rawType, genericTypeArguments, constructorArgs);
    }
}

class ConstructorInfo {
    final Constructor<?> constructor;
    final Class<?>[] parameterTypes;
    final Type[] genericParameterTypes;
    final Iterable<Annotation> parameterAnnotations;

    ConstructorInfo(Constructor<?> constructor,
                    Class<?>[] parameterTypes,
                    Type[] genericParameterTypes,
                    Iterable<Annotation> parameterAnnotations) {
        this.constructor = constructor;
        this.parameterTypes = parameterTypes;
        this.genericParameterTypes = genericParameterTypes;
        this.parameterAnnotations = parameterAnnotations;
    }
}

class JsonDeserializationException extends RuntimeException {
    JsonDeserializationException(String message) {
        super(message);
    }

    JsonDeserializationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}