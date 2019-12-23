package com.jeppeman.locallydynamic.net;

public class Header {
    private final String name;
    private final String value;

    private Header(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public static class Builder {
        private String name = "";
        private String value = "";

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        public Header build() {
            return new Header(name, value);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}