package com.jeppeman.globallydynamic.serialization;

import java.util.Arrays;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class JsonParser {
    static final Pattern LITERAL_REGEX = Pattern.compile("([0-9]+(\\.[0-9]+)?)|null|true|false");

    void parse(String json, Callbacks callbacks) {
        JsonParserContext context = new JsonParserContext(callbacks, json);
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            context.parseChar(c, i);
        }
        context.endOfInputReached();
    }

    interface Callbacks {
        void keyParsed(String key);

        void primitiveParsed(String value);

        void beginObject();

        void endObject();

        void beginArray();

        void endArray();
    }
}

class JsonParserContext {
    private final JsonParser.Callbacks callbacks;
    private final String inputString;
    private Stack<State> states = new Stack<State>();
    private StringBuilder currentKey = new StringBuilder();
    private StringBuilder currentValue = new StringBuilder();

    JsonParserContext(
            JsonParser.Callbacks callbacks,
            String inputString) {
        this.callbacks = callbacks;
        this.inputString = inputString;
        states.push(Initial.INSTANCE);
    }


    private State getState() {
        return states.peek();
    }

    private State getPreviousState() {
        return states.get(states.size() - 2);
    }

    private void flushCurrentKey() {
        String key = currentKey.toString();
        callbacks.keyParsed(key);
        currentKey = new StringBuilder();
    }

    private void flushCurrentValue() {
        String value = currentValue.toString();
        callbacks.primitiveParsed(value);
        currentValue = new StringBuilder();
    }

    private void popCurrent() {
        states.pop();
    }

    private void endObject() {
        popCurrent();
        callbacks.endObject();
        if (getState() != Initial.INSTANCE) {
            states.push(ValueBuilt.INSTANCE);
        } else {
            popCurrent();
            states.push(Finalized.INSTANCE);
        }
    }

    private void endArray() {
        popCurrent();
        callbacks.endArray();
        if (getState() != Initial.INSTANCE) {
            states.push(ValueBuilt.INSTANCE);
        } else {
            popCurrent();
            states.push(Finalized.INSTANCE);
        }
    }

    void parseChar(char c, int index) {
        getState().parseChar(this, c, index);
    }

    void endOfInputReached() {
        getState().endOfInputReached();
    }

    static abstract class State {
        abstract void parseChar(JsonParserContext context, char c, int index);

        void endOfInputReached() {
            throw new JsonParseException("Unexpected end of input reached");
        }
    }

    static class Initial extends State {
        static final Initial INSTANCE = new Initial();

        private Initial() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (Character.isWhitespace(c)) {
                return;
            } else if (JsonToken.BEGIN_OBJECT.getValue() == c) {
                context.states.push(BeginObject.INSTANCE);
                context.callbacks.beginObject();
            } else if (JsonToken.BEGIN_ARRAY.getValue() == c) {
                context.states.push(BeginArray.INSTANCE);
                context.callbacks.beginArray();
            } else {
                throw new JsonParseException(c, index, JsonToken.BEGIN_OBJECT.getValue(),
                        JsonToken.BEGIN_ARRAY.getValue());
            }
        }
    }

    static class BeginObject extends State {
        static final BeginObject INSTANCE = new BeginObject();

        private BeginObject() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (Character.isWhitespace(c)) {
                return;
            } else if (JsonToken.DOUBLE_QUOTE.getValue() == c) {
                context.states.push(BuildingKeyDouble.INSTANCE);
            } else if (JsonToken.SINGLE_QUOTE.getValue() == c) {
                context.states.push(BuildingKeySingle.INSTANCE);
            } else if (JsonToken.END_OBJECT.getValue() == c) {
                context.endObject();
            } else {
                context.currentKey.append(c);
                context.states.push(BuildingKey.INSTANCE);
            }
        }
    }

    static class BeginArray extends State {
        static final BeginArray INSTANCE = new BeginArray();

        private BeginArray() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (Character.isWhitespace(c)) {
                return;
            } else if (JsonToken.DOUBLE_QUOTE.getValue() == c) {
                context.states.push(BuildingStringValueDouble.INSTANCE);
            } else if (JsonToken.SINGLE_QUOTE.getValue() == c) {
                context.states.push(BuildingStringValueSingle.INSTANCE);
            } else if (JsonToken.END_ARRAY.getValue() == c) {
                context.endArray();
            } else if (JsonToken.BEGIN_ARRAY.getValue() == c) {
                context.callbacks.beginArray();
                context.states.push(BeginArray.INSTANCE);
            } else if (JsonToken.BEGIN_OBJECT.getValue() == c) {
                context.callbacks.beginObject();
                context.states.push(BeginObject.INSTANCE);
            } else {
                context.currentValue.append(c);
                context.states.push(BuildingLiteralValue.INSTANCE);
            }
        }
    }

    static class BuildingKey extends State {
        static final BuildingKey INSTANCE = new BuildingKey();

        private BuildingKey() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (Character.isWhitespace(c)) {
                context.popCurrent();
                context.states.push(KeyBuilt.INSTANCE);
                context.flushCurrentKey();
            } else if (JsonToken.COLON.getValue() == c) {
                context.popCurrent();
                context.states.push(ColonReceived.INSTANCE);
                context.flushCurrentKey();
            } else {
                context.currentKey.append(c);
            }
        }
    }

    static class BuildingKeySingle extends State {
        static final BuildingKeySingle INSTANCE = new BuildingKeySingle();

        private BuildingKeySingle() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (JsonToken.SINGLE_QUOTE.getValue() == c) {
                context.popCurrent();
                context.states.push(KeyBuilt.INSTANCE);
                context.flushCurrentKey();
            } else {
                context.currentKey.append(c);
            }
        }
    }

    static class BuildingKeyDouble extends State {
        static final BuildingKeyDouble INSTANCE = new BuildingKeyDouble();

        private BuildingKeyDouble() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (JsonToken.DOUBLE_QUOTE.getValue() == c) {
                context.popCurrent();
                context.states.push(KeyBuilt.INSTANCE);
                context.flushCurrentKey();
            } else {
                context.currentKey.append(c);
            }
        }
    }

    static class KeyBuilt extends State {
        static final KeyBuilt INSTANCE = new KeyBuilt();

        private KeyBuilt() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (Character.isWhitespace(c)) {
                return;
            } else if (JsonToken.COLON.getValue() == c) {
                context.popCurrent();
                context.states.push(ColonReceived.INSTANCE);
            } else {
                throw new JsonParseException(c, index, JsonToken.COLON.getValue());
            }
        }
    }

    static class BuildingStringValueSingle extends State {
        static final BuildingStringValueSingle INSTANCE = new BuildingStringValueSingle();

        private BuildingStringValueSingle() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (context.inputString.charAt(index - 1) != '\\'
                    && JsonToken.SINGLE_QUOTE.getValue() == c) {
                context.flushCurrentValue();
                context.popCurrent();
                context.states.push(ValueBuilt.INSTANCE);
            } else {
                context.currentValue.append(c);
            }
        }
    }

    static class BuildingStringValueDouble extends State {
        static final BuildingStringValueDouble INSTANCE = new BuildingStringValueDouble();

        private BuildingStringValueDouble() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (context.inputString.charAt(index - 1) != '\\'
                    && JsonToken.DOUBLE_QUOTE.getValue() == c) {
                context.flushCurrentValue();
                context.popCurrent();
                context.states.push(ValueBuilt.INSTANCE);
            } else {
                context.currentValue.append(c);
            }
        }
    }

    static class BuildingLiteralValue extends State {
        static final BuildingLiteralValue INSTANCE = new BuildingLiteralValue();

        private BuildingLiteralValue() {

        }

        private void validateLiteral(String literal, int index) {
            Matcher matcher = JsonParser.LITERAL_REGEX.matcher(literal);
            if (!matcher.matches()) {
                throw new JsonParseException("Invalid literal \"" + literal + "\", at index " + (index - literal.length()));
            }
        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (Character.isWhitespace(c)) {
                validateLiteral(context.currentValue.toString(), index);
                context.flushCurrentValue();
                context.popCurrent();
                context.states.push(ValueBuilt.INSTANCE);
            } else if (JsonToken.COMMA.getValue() == c) {
                validateLiteral(context.currentValue.toString(), index);
                context.flushCurrentValue();
                context.popCurrent();
            } else if (JsonToken.END_OBJECT.getValue() == c
                    && context.getPreviousState() == BeginObject.INSTANCE) {
                validateLiteral(context.currentValue.toString(), index);
                context.flushCurrentValue();
                context.popCurrent();
                context.endObject();
            } else if (JsonToken.END_ARRAY.getValue() == c
                    && context.getPreviousState() == BeginArray.INSTANCE) {
                validateLiteral(context.currentValue.toString(), index);
                context.flushCurrentValue();
                context.popCurrent();
                context.endArray();
            } else {
                context.currentValue.append(c);
            }
        }
    }

    static class ValueBuilt extends State {
        static final ValueBuilt INSTANCE = new ValueBuilt();

        private ValueBuilt() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (Character.isWhitespace(c)) {
                return;
            } else if (JsonToken.COMMA.getValue() == c) {
                context.popCurrent();
            } else if (JsonToken.END_OBJECT.getValue() == c
                    && context.getPreviousState() == BeginObject.INSTANCE) {
                context.popCurrent();
                context.endObject();
            } else if (JsonToken.END_ARRAY.getValue() == c
                    && context.getPreviousState() == BeginArray.INSTANCE) {
                context.popCurrent();
                context.endArray();
            } else {
                throw new JsonParseException(c, index, JsonToken.COMMA.getValue(),
                        JsonToken.END_OBJECT.getValue());
            }
        }
    }

    static class ColonReceived extends State {
        static final ColonReceived INSTANCE = new ColonReceived();

        private ColonReceived() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            if (Character.isWhitespace(c)) {
                return;
            } else if (JsonToken.SINGLE_QUOTE.getValue() == c) {
                context.popCurrent();
                context.states.push(BuildingStringValueSingle.INSTANCE);
            } else if (JsonToken.DOUBLE_QUOTE.getValue() == c) {
                context.popCurrent();
                context.states.push(BuildingStringValueDouble.INSTANCE);
            } else if (JsonToken.BEGIN_OBJECT.getValue() == c) {
                context.callbacks.beginObject();
                context.popCurrent();
                context.states.push(BeginObject.INSTANCE);
            } else if (JsonToken.BEGIN_ARRAY.getValue() == c) {
                context.callbacks.beginArray();
                context.popCurrent();
                context.states.push(BeginArray.INSTANCE);
            } else {
                context.currentValue.append(c);
                context.popCurrent();
                context.states.push(BuildingLiteralValue.INSTANCE);
            }
        }
    }

    static class Finalized extends State {
        static final Finalized INSTANCE = new Finalized();

        private Finalized() {

        }

        @Override
        public void parseChar(JsonParserContext context, char c, int index) {
            throw new JsonParseException(c, index);
        }

        @Override
        public void endOfInputReached() {
            // Do nothing
        }
    }
}

class JsonParseException extends RuntimeException {
    JsonParseException(String message) {
        super(message);
    }

    JsonParseException(char c, int index, char... allowedChars) {
        this(String.format(
                Locale.ENGLISH,
                "Unexpected character (%c) at index %d%s",
                c,
                index,
                allowedChars.length > 0
                        ? ", expected one of [" + StringUtils.joinToString(Arrays.asList(allowedChars), ",")
                        : ""
        ));
    }
}