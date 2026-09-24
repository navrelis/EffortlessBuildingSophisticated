package sophisticated.building.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A single config option. Created through {@link ConfigSpec.Builder}; read and written as JSON by
 * {@link ConfigSpec}.
 */
public abstract class SimpleConfigValue<T> {
    private final String key;
    private final String comment;
    private final T defaultValue;
    private volatile T value;
    private boolean synced = true;

    protected SimpleConfigValue(String key, String comment, T defaultValue) {
        this.key = key;
        this.comment = comment;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    /** False for server-only values that are never sent to clients. */
    public boolean isSynced() {
        return synced;
    }

    void markNotSynced() {
        synced = false;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public T getDefault() {
        return defaultValue;
    }

    public String getKey() {
        return key;
    }

    /** NeoForge comment text plus range/default, as written into the JSON file. */
    public String getComment() {
        String extra = describeRange();
        String base = comment == null || comment.isEmpty() ? "" : comment + " ";
        return base + (extra.isEmpty() ? "" : extra + " ") + "Default: " + defaultValue;
    }

    void reset() {
        value = defaultValue;
    }

    protected String describeRange() {
        return "";
    }

    abstract JsonElement toJson(T value);

    /**
     * Parses and applies a JSON value. Invalid values fall back to the default, out-of-range
     * numbers are clamped; both report a message to {@code warn}.
     */
    abstract void read(JsonElement json, String path, Consumer<String> warn);

    JsonElement toJson() {
        return toJson(value);
    }

    public static final class BooleanValue extends SimpleConfigValue<Boolean> {
        BooleanValue(String key, String comment, boolean defaultValue) {
            super(key, comment, defaultValue);
        }

        @Override
        JsonElement toJson(Boolean value) {
            return new JsonPrimitive(value);
        }

        @Override
        void read(JsonElement json, String path, Consumer<String> warn) {
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) {
                set(json.getAsBoolean());
            } else {
                warn.accept(path + " must be true or false, using default " + getDefault());
                reset();
            }
        }
    }

    public static final class IntValue extends SimpleConfigValue<Integer> {
        private final int min;
        private final int max;

        IntValue(String key, String comment, int defaultValue, int min, int max) {
            super(key, comment, defaultValue);
            this.min = min;
            this.max = max;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        @Override
        protected String describeRange() {
            return "Range: " + min + " ~ " + max + ".";
        }

        @Override
        JsonElement toJson(Integer value) {
            return new JsonPrimitive(value);
        }

        @Override
        void read(JsonElement json, String path, Consumer<String> warn) {
            BigDecimal number = asNumber(json);
            if (number == null || number.stripTrailingZeros().scale() > 0) {
                warn.accept(path + " must be a whole number, using default " + getDefault());
                reset();
                return;
            }
            if (number.compareTo(BigDecimal.valueOf(min)) < 0 || number.compareTo(BigDecimal.valueOf(max)) > 0) {
                int clamped = number.compareTo(BigDecimal.valueOf(min)) < 0 ? min : max;
                warn.accept(path + " = " + number.toPlainString() + " is out of range [" + min + ", " + max + "], clamped to " + clamped);
                set(clamped);
                return;
            }
            set(number.intValueExact());
        }
    }

    public static final class DoubleValue extends SimpleConfigValue<Double> {
        private final double min;
        private final double max;

        DoubleValue(String key, String comment, double defaultValue, double min, double max) {
            super(key, comment, defaultValue);
            this.min = min;
            this.max = max;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        @Override
        protected String describeRange() {
            return "Range: " + min + " ~ " + max + ".";
        }

        @Override
        JsonElement toJson(Double value) {
            return new JsonPrimitive(value);
        }

        @Override
        void read(JsonElement json, String path, Consumer<String> warn) {
            BigDecimal number = asNumber(json);
            if (number == null) {
                warn.accept(path + " must be a number, using default " + getDefault());
                reset();
                return;
            }
            double parsed = number.doubleValue();
            if (parsed < min || parsed > max) {
                double clamped = parsed < min ? min : max;
                warn.accept(path + " = " + number.toPlainString() + " is out of range [" + min + ", " + max + "], clamped to " + clamped);
                set(clamped);
                return;
            }
            set(parsed);
        }
    }

    public static final class StringListValue extends SimpleConfigValue<List<String>> {
        StringListValue(String key, String comment, List<String> defaultValue) {
            super(key, comment, List.copyOf(defaultValue));
        }

        @Override
        JsonElement toJson(List<String> value) {
            JsonArray array = new JsonArray();
            value.forEach(array::add);
            return array;
        }

        @Override
        void read(JsonElement json, String path, Consumer<String> warn) {
            if (!json.isJsonArray()) {
                warn.accept(path + " must be a list of strings, using default " + getDefault());
                reset();
                return;
            }
            List<String> parsed = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    parsed.add(element.getAsString());
                } else {
                    warn.accept(path + " contains a non-string entry, it is ignored");
                }
            }
            set(List.copyOf(parsed));
        }
    }

    private static BigDecimal asNumber(JsonElement json) {
        if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        try {
            return json.getAsJsonPrimitive().getAsBigDecimal();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
