package dev.infrai.legaltech.storage;

import java.util.LinkedHashMap;
import java.util.Map;

final class JsonCodec {
    private JsonCodec() {}

    static String object(Map<String, ?> values) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) out.append(',');
            first = false;
            out.append(quote(entry.getKey())).append(':');
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) out.append(value);
            else out.append(quote(String.valueOf(value)));
        }
        return out.append('}').toString();
    }

    static Map<String, Object> parseObject(String json) {
        Object value = new Parser(json).parseValue();
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("Expected JSON object");
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.append('"').toString();
    }

    private static final class Parser {
        private final String text;
        private int at;

        Parser(String text) { this.text = text; }

        Object parseValue() {
            whitespace();
            if (at >= text.length()) throw new IllegalArgumentException("Unexpected end of JSON");
            return switch (text.charAt(at)) {
                case '{' -> parseMap();
                case '"' -> parseString();
                case 't' -> literal("true", true);
                case 'f' -> literal("false", false);
                case 'n' -> literal("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            at++;
            whitespace();
            if (take('}')) return map;
            do {
                whitespace();
                String key = parseString();
                whitespace();
                require(':');
                map.put(key, parseValue());
                whitespace();
            } while (take(','));
            require('}');
            return map;
        }

        private String parseString() {
            require('"');
            StringBuilder out = new StringBuilder();
            while (at < text.length()) {
                char c = text.charAt(at++);
                if (c == '"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                char escaped = text.charAt(at++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                        at += 4;
                    }
                    default -> throw new IllegalArgumentException("Invalid JSON escape");
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private Object parseNumber() {
            int start = at;
            while (at < text.length() && "-+0123456789.eE".indexOf(text.charAt(at)) >= 0) at++;
            String number = text.substring(start, at);
            try { return number.contains(".") ? Double.parseDouble(number) : Long.parseLong(number); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid JSON value", e); }
        }

        private Object literal(String expected, Object value) {
            if (!text.startsWith(expected, at)) throw new IllegalArgumentException("Invalid JSON literal");
            at += expected.length();
            return value;
        }

        private void whitespace() {
            while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++;
        }

        private boolean take(char expected) {
            if (at < text.length() && text.charAt(at) == expected) { at++; return true; }
            return false;
        }

        private void require(char expected) {
            if (!take(expected)) throw new IllegalArgumentException("Expected '" + expected + "'");
        }
    }
}
