package multiroute;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small, strict JSON codec with no third-party dependencies. */
public final class Json {
    private Json() { }

    public static Object parse(String input) {
        if (input == null) throw new IllegalArgumentException("JSON input is null");
        Parser parser = new Parser(input);
        Object value = parser.value(0);
        parser.space();
        if (parser.pos != input.length()) throw parser.error("Trailing data");
        return value;
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out, int depth) {
        if (depth > 64) throw new IllegalArgumentException("JSON nesting too deep");
        if (value == null) { out.append("null"); return; }
        if (value instanceof String text) { quote(text, out); return; }
        if (value instanceof Boolean) { out.append(value); return; }
        if (value instanceof Number number) {
            if ((number instanceof Double d && !Double.isFinite(d)) ||
                    (number instanceof Float f && !Float.isFinite(f))) {
                throw new IllegalArgumentException("Nonfinite JSON number");
            }
            out.append(number);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String)) throw new IllegalArgumentException("JSON object key must be string");
                if (!first) out.append(',');
                quote((String) entry.getKey(), out);
                out.append(':');
                write(entry.getValue(), out, depth + 1);
                first = false;
            }
            out.append('}');
            return;
        }
        if (value instanceof Iterable<?> list) {
            out.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) out.append(',');
                write(item, out, depth + 1);
                first = false;
            }
            out.append(']');
            return;
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass().getName());
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else if (Character.isHighSurrogate(c)) {
                        if (i + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(i + 1)))
                            throw new IllegalArgumentException("Unpaired high surrogate");
                        out.append(c).append(text.charAt(++i));
                    } else if (Character.isLowSurrogate(c)) {
                        throw new IllegalArgumentException("Unpaired low surrogate");
                    } else out.append(c);
                }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String input;
        private int pos;

        Parser(String input) { this.input = input; }

        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at JSON offset " + pos);
        }

        void space() {
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') pos++;
                else break;
            }
        }

        Object value(int depth) {
            if (depth > 64) throw error("JSON nesting too deep");
            space();
            if (pos >= input.length()) throw error("Expected value");
            return switch (input.charAt(pos)) {
                case '{' -> object(depth + 1);
                case '[' -> array(depth + 1);
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> {
                    char c = input.charAt(pos);
                    if (c == '-' || (c >= '0' && c <= '9')) yield number();
                    throw error("Expected value");
                }
            };
        }

        Object literal(String token, Object result) {
            if (!input.startsWith(token, pos)) throw error("Bad literal");
            pos += token.length();
            return result;
        }

        Map<String, Object> object(int depth) {
            pos++;
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (take('}')) return result;
            while (true) {
                space();
                if (pos >= input.length() || input.charAt(pos) != '"') throw error("Expected object key");
                String key = string();
                space();
                if (!take(':')) throw error("Expected colon");
                if (result.containsKey(key)) throw error("Duplicate object key");
                result.put(key, value(depth));
                space();
                if (take('}')) return result;
                if (!take(',')) throw error("Expected comma");
            }
        }

        List<Object> array(int depth) {
            pos++;
            List<Object> result = new ArrayList<>();
            space();
            if (take(']')) return result;
            while (true) {
                result.add(value(depth));
                space();
                if (take(']')) return result;
                if (!take(',')) throw error("Expected comma");
            }
        }

        boolean take(char expected) {
            if (pos < input.length() && input.charAt(pos) == expected) { pos++; return true; }
            return false;
        }

        String string() {
            pos++;
            StringBuilder out = new StringBuilder();
            while (pos < input.length()) {
                char c = input.charAt(pos++);
                if (c == '"') return out.toString();
                if (c < 0x20) throw error("Control character in string");
                if (c == '\\') {
                    if (pos >= input.length()) throw error("Incomplete escape");
                    c = input.charAt(pos++);
                    switch (c) {
                        case '"', '\\', '/' -> out.append(c);
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            char unit = unicode();
                            if (Character.isHighSurrogate(unit)) {
                                if (pos + 1 >= input.length() || input.charAt(pos++) != '\\' || input.charAt(pos++) != 'u')
                                    throw error("Unpaired high surrogate");
                                char low = unicode();
                                if (!Character.isLowSurrogate(low)) throw error("Unpaired high surrogate");
                                out.append(unit).append(low);
                            } else if (Character.isLowSurrogate(unit)) throw error("Unpaired low surrogate");
                            else out.append(unit);
                        }
                        default -> throw error("Bad escape");
                    }
                } else if (Character.isHighSurrogate(c)) {
                    if (pos >= input.length() || !Character.isLowSurrogate(input.charAt(pos)))
                        throw error("Unpaired high surrogate");
                    out.append(c).append(input.charAt(pos++));
                } else if (Character.isLowSurrogate(c)) throw error("Unpaired low surrogate");
                else out.append(c);
            }
            throw error("Unterminated string");
        }

        char unicode() {
            if (pos + 4 > input.length()) throw error("Incomplete Unicode escape");
            int code = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(input.charAt(pos++), 16);
                if (digit < 0) throw error("Bad Unicode escape");
                code = (code << 4) | digit;
            }
            return (char) code;
        }

        Number number() {
            int start = pos;
            take('-');
            if (take('0')) {
                if (pos < input.length() && Character.isDigit(input.charAt(pos))) throw error("Leading zero");
            } else {
                if (pos >= input.length() || input.charAt(pos) < '1' || input.charAt(pos) > '9') throw error("Bad number");
                while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') pos++;
            }
            boolean decimal = false;
            if (take('.')) {
                decimal = true;
                digits();
            }
            if (take('e') || take('E')) {
                decimal = true;
                if (!take('+')) take('-');
                digits();
            }
            String raw = input.substring(start, pos);
            try { return decimal ? new BigDecimal(raw) : Long.parseLong(raw); }
            catch (NumberFormatException e) { return new BigDecimal(raw); }
        }

        void digits() {
            int start = pos;
            while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') pos++;
            if (start == pos) throw error("Expected digit");
        }
    }
}
