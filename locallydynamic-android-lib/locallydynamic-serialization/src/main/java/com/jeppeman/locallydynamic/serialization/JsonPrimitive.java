package com.jeppeman.locallydynamic.serialization;

import java.math.BigDecimal;

public class JsonPrimitive extends JsonElement {
    private final Object value;

    private JsonPrimitive(Object value) {
        this.value = value;
    }

    public JsonPrimitive(boolean value) {
        this((Object) value);
    }

    public JsonPrimitive(int value) {
        this((Object) value);
    }

    public JsonPrimitive(long value) {
        this((Object) value);
    }

    public JsonPrimitive(double value) {
        this((Object) value);
    }

    public JsonPrimitive(float value) {
        this((Object) value);
    }

    public JsonPrimitive(short value) {
        this((Object) value);
    }

    public JsonPrimitive(String value) {
        this((Object) value);
    }

    public JsonPrimitive(BigDecimal value) {
        this((Object) value);
    }

    public BigDecimal getAsBigDecimal() {
        return new BigDecimal(value.toString());
    }

    public boolean getAsBoolean() {
        return Boolean.parseBoolean(value.toString());
    }

    public int getAsInt() {
        return Integer.parseInt(value.toString());
    }

    public long getAsLong() {
        return Long.parseLong(value.toString());
    }

    public float getAsFloat() {
        return Float.parseFloat(value.toString());
    }

    public double getAsDouble() {
        return Double.parseDouble(value.toString());
    }

    public short getAsShort() {
        return Short.parseShort(value.toString());
    }

    public String getAsString() {
        return value.toString();
    }

    @Override
    public String toString() {
        return value instanceof String
                ? "\"" + value + "\""
                : value.toString();
    }
}