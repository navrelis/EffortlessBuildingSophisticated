package sophisticated.building.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Schema of one JSON config file: sections (NeoForge {@code push} names) holding options
 * (NeoForge {@code define} names). Pure JSON logic, no file or Minecraft access.
 */
public final class ConfigSpec {
    public static final String COMMENT_KEY = "_comment";

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    private final String name;
    private final List<Section> sections;

    private ConfigSpec(String name, List<Section> sections) {
        this.name = name;
        this.sections = sections;
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return "sophisticatedbuilding-" + name + ".json";
    }

    public void resetToDefaults() {
        for (Section section : sections) {
            section.values.forEach(SimpleConfigValue::reset);
        }
    }

    /**
     * Applies {@code json} to all values. Missing or invalid values get their default. On a parse
     * error every value is reset to its default.
     */
    public LoadResult load(String json) {
        return load(json, false);
    }

    /**
     * Applies JSON produced by {@link #toSyncJson()}. Only synced values are touched; values that
     * are not synced keep their current local value.
     */
    public LoadResult loadSync(String json) {
        return load(json, true);
    }

    private LoadResult load(String json, boolean syncOnly) {
        JsonObject root;
        try {
            // Minecraft 1.16.3 ships Gson 2.8.0, which has no static JsonParser.parseString
            JsonElement parsed = new JsonParser().parse(json);
            if (!parsed.isJsonObject()) {
                throw new JsonParseException("top level is not a JSON object");
            }
            root = parsed.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            for (Section section : sections) {
                section.values.stream().filter(value -> !syncOnly || value.isSynced()).forEach(SimpleConfigValue::reset);
            }
            return new LoadResult(false, false, Collections.emptyList(), e.getMessage(), Collections.emptyList());
        }

        List<String> warnings = new ArrayList<>();
        List<String> unknownKeys = new ArrayList<>();
        boolean missingKeys = false;
        for (Section section : sections) {
            List<SimpleConfigValue<?>> values = syncOnly
                    ? section.values.stream().filter(SimpleConfigValue::isSynced).collect(Collectors.toList())
                    : section.values;
            if (values.isEmpty()) {
                continue;
            }
            JsonElement sectionJson = root.get(section.name);
            if (sectionJson == null) {
                missingKeys = true;
                values.forEach(SimpleConfigValue::reset);
                continue;
            }
            if (!sectionJson.isJsonObject()) {
                warnings.add(section.name + " must be a JSON object, using defaults for this section");
                values.forEach(SimpleConfigValue::reset);
                continue;
            }
            JsonObject sectionObject = sectionJson.getAsJsonObject();
            for (SimpleConfigValue<?> value : values) {
                JsonElement valueJson = sectionObject.get(value.getKey());
                if (valueJson == null || valueJson.isJsonNull()) {
                    missingKeys = true;
                    value.reset();
                } else {
                    value.read(valueJson, section.name + "." + value.getKey(), warnings::add);
                }
            }
            if (!syncOnly) {
                unknownKeys.addAll(findUnknownKeys(section, sectionObject));
            }
        }
        return new LoadResult(true, missingKeys, Collections.unmodifiableList(warnings), null, Collections.unmodifiableList(unknownKeys));
    }

    /** Keys present in {@code sectionObject} that are not one of {@code section}'s defined options. */
    private static List<String> findUnknownKeys(Section section, JsonObject sectionObject) {
        Set<String> knownKeys = new HashSet<>();
        for (SimpleConfigValue<?> value : section.values) {
            knownKeys.add(value.getKey());
        }
        List<String> unknown = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : sectionObject.entrySet()) { // Gson 2.8.0: no JsonObject.keySet
            String key = entry.getKey();
            if (key.equals(COMMENT_KEY) || key.startsWith(COMMENT_KEY + "_")) {
                continue;
            }
            if (!knownKeys.contains(key)) {
                unknown.add(section.name + "." + key);
            }
        }
        return unknown;
    }

    /** Pretty-printed file content with comments and the current values. */
    public String toFileJson() {
        return PRETTY.toJson(toJsonObject(true));
    }

    /** Compact values-only JSON without server-only values, used for the server to client sync. */
    public String toSyncJson() {
        return COMPACT.toJson(toJsonObject(false));
    }

    private JsonObject toJsonObject(boolean forFile) {
        JsonObject root = new JsonObject();
        for (Section section : sections) {
            JsonObject sectionObject = new JsonObject();
            if (forFile && section.comment != null) {
                sectionObject.addProperty(COMMENT_KEY, section.comment);
            }
            for (SimpleConfigValue<?> value : section.values) {
                if (!forFile && !value.isSynced()) {
                    continue;
                }
                if (forFile) {
                    sectionObject.addProperty(COMMENT_KEY + "_" + value.getKey(), value.getComment());
                }
                sectionObject.add(value.getKey(), value.toJson());
            }
            root.add(section.name, sectionObject);
        }
        return root;
    }

    /**
     * @param parsed      false if the JSON could not be parsed (all values were reset to defaults)
     * @param missingKeys true if at least one option was missing and got its default
     * @param unknownKeys keys present in the JSON that are not defined options (e.g. old/typo'd
     *                    keys); they are dropped when the file is rewritten
     */
    public static final class LoadResult {
        private final boolean parsed;
        private final boolean missingKeys;
        private final List<String> warnings;
        private final String error;
        private final List<String> unknownKeys;

        public LoadResult(boolean parsed, boolean missingKeys, List<String> warnings, String error, List<String> unknownKeys) {
            this.parsed = parsed;
            this.missingKeys = missingKeys;
            this.warnings = warnings;
            this.error = error;
            this.unknownKeys = unknownKeys;
        }

        public boolean parsed() {
            return parsed;
        }

        public boolean missingKeys() {
            return missingKeys;
        }

        public List<String> warnings() {
            return warnings;
        }

        public String error() {
            return error;
        }

        public List<String> unknownKeys() {
            return unknownKeys;
        }

        /** True if loading found anything that requires the file to be corrected and backed up. */
        public boolean needsCorrection() {
            return missingKeys || !warnings.isEmpty() || !unknownKeys.isEmpty();
        }
    }

    private static final class Section {
        private final String name;
        private final String comment;
        private final List<SimpleConfigValue<?>> values;

        public Section(String name, String comment, List<SimpleConfigValue<?>> values) {
            this.name = name;
            this.comment = comment;
            this.values = values;
        }

        public String name() {
            return name;
        }

        public String comment() {
            return comment;
        }

        public List<SimpleConfigValue<?>> values() {
            return values;
        }
    }

    /** Mirrors the NeoForge {@code ModConfigSpec.Builder} calls used by the config classes. */
    public static final class Builder implements IConfigBuilder {
        private final String name;
        private final List<Section> sections = new ArrayList<>();
        private Section current;
        private String pendingComment;
        private boolean pendingNotSynced;

        public Builder(String name) {
            this.name = name;
        }

        @Override
        public Builder comment(String... lines) {
            pendingComment = String.join(" ", lines);
            return this;
        }

        /** The next defined value is server-only and left out of {@link ConfigSpec#toSyncJson()}. */
        @Override
        public Builder notSynced() {
            pendingNotSynced = true;
            return this;
        }

        @Override
        public Builder push(String section) {
            if (current != null) {
                throw new IllegalStateException("Nested config sections are not supported: " + section);
            }
            current = new Section(section, takeComment(), new ArrayList<>());
            sections.add(current);
            return this;
        }

        @Override
        public Builder pop() {
            current = null;
            return this;
        }

        /** A world restart is a NeoForge concept; the Fabric config has none. */
        @Override
        public Builder worldRestart() {
            return this;
        }

        @Override
        public SimpleConfigValue<Boolean> define(String key, boolean defaultValue) {
            return add(new SimpleConfigValue.BooleanValue(key, takeComment(), defaultValue));
        }

        @Override
        public SimpleConfigValue<List<String>> defineList(String key, List<String> defaultValue) {
            return add(new SimpleConfigValue.StringListValue(key, takeComment(), defaultValue));
        }

        @Override
        public SimpleConfigValue.IntValue defineInRange(String key, int defaultValue, int min, int max) {
            return add(new SimpleConfigValue.IntValue(key, takeComment(), defaultValue, min, max));
        }

        @Override
        public SimpleConfigValue.DoubleValue defineInRange(String key, double defaultValue, double min, double max) {
            return add(new SimpleConfigValue.DoubleValue(key, takeComment(), defaultValue, min, max));
        }

        @Override
        public ConfigSpec build() {
            if (current != null) {
                throw new IllegalStateException("Config section not popped: " + current.name);
            }
            return new ConfigSpec(name, Collections.unmodifiableList(new ArrayList<>(sections)));
        }

        private <V extends SimpleConfigValue<?>> V add(V value) {
            if (current == null) {
                throw new IllegalStateException("Config value defined outside a section: " + value.getKey());
            }
            if (pendingNotSynced) {
                value.markNotSynced();
                pendingNotSynced = false;
            }
            current.values.add(value);
            return value;
        }

        private String takeComment() {
            String comment = pendingComment;
            pendingComment = null;
            return comment;
        }
    }
}
