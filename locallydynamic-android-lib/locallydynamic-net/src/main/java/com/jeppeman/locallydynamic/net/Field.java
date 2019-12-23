package com.jeppeman.locallydynamic.net;

public class Field {
    private final String name;
    private final Object value;

    private Field(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public static class Builder {
        private String name = "";
        private Object value = "";

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setValue(Object value) {
            this.value = value;
            return this;
        }

        public Field build() {
            return new Field(
                StringUtils.urlEncode(name),
                StringUtils.urlEncode(value.toString())
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}