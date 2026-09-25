package sophisticated.building.forge.platform;

import net.minecraftforge.common.ForgeConfigSpec;
import sophisticated.building.config.ConfigType;
import sophisticated.building.config.ConfigValue;
import sophisticated.building.config.IConfigBuilder;
import sophisticated.building.config.NumberConfigValue;
import sophisticated.building.platform.services.IConfigHelper;

import java.util.List;

/**
 * Forge config backend: the options become {@link ForgeConfigSpec} values (TOML files with the same
 * names, sections and keys as on NeoForge; the SERVER config is per world and synced to the clients).
 * The specs are registered by {@code SophisticatedBuildingForge}.
 */
public final class ForgeConfigHelper implements IConfigHelper {

    @Override
    public IConfigBuilder createBuilder(ConfigType type) {
        return new Builder(new ForgeConfigSpec.Builder());
    }

    /** Writes the values set through {@link ConfigValue#set} to the TOML file of the (loaded) config. */
    @Override
    public void save(Object spec) {
        ((ForgeConfigSpec) spec).save();
    }

    private record Builder(ForgeConfigSpec.Builder builder) implements IConfigBuilder {

        @Override
        public IConfigBuilder comment(String... lines) {
            builder.comment(lines);
            return this;
        }

        @Override
        public IConfigBuilder push(String section) {
            builder.push(section);
            return this;
        }

        @Override
        public IConfigBuilder pop() {
            builder.pop();
            return this;
        }

        @Override
        public IConfigBuilder worldRestart() {
            builder.worldRestart();
            return this;
        }

        /** Every Forge config value is only read on its own side; there is no sync to leave out. */
        @Override
        public IConfigBuilder notSynced() {
            return this;
        }

        @Override
        public ConfigValue<Boolean> define(String key, boolean defaultValue) {
            return new Value<>(builder.define(key, defaultValue));
        }

        @Override
        @SuppressWarnings("unchecked")
        public ConfigValue<List<String>> defineList(String key, List<String> defaultValue) {
            ForgeConfigSpec.ConfigValue<List<? extends String>> value = builder.defineList(key, defaultValue, o -> true);
            return new ConfigValue<>() {
                @Override
                public List<String> get() {
                    return (List<String>) value.get();
                }

                @Override
                public void set(List<String> newValue) {
                    value.set(newValue);
                }

                @Override
                public List<String> getDefault() {
                    return (List<String>) value.getDefault();
                }
            };
        }

        @Override
        public NumberConfigValue<Integer> defineInRange(String key, int defaultValue, int min, int max) {
            return new NumberValue<>(builder.defineInRange(key, defaultValue, min, max), min, max);
        }

        @Override
        public NumberConfigValue<Double> defineInRange(String key, double defaultValue, double min, double max) {
            return new NumberValue<>(builder.defineInRange(key, defaultValue, min, max), min, max);
        }

        @Override
        public ForgeConfigSpec build() {
            return builder.build();
        }
    }

    /** A loader-neutral view of a ForgeConfigSpec value; set() changes the loaded config in memory. */
    private static class Value<T> implements ConfigValue<T> {
        private final ForgeConfigSpec.ConfigValue<T> value;

        Value(ForgeConfigSpec.ConfigValue<T> value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value.get();
        }

        @Override
        public void set(T newValue) {
            value.set(newValue);
        }

        @Override
        public T getDefault() {
            return value.getDefault();
        }
    }

    private static final class NumberValue<T extends Number & Comparable<? super T>> extends Value<T> implements NumberConfigValue<T> {
        private final T min;
        private final T max;

        NumberValue(ForgeConfigSpec.ConfigValue<T> value, T min, T max) {
            super(value);
            this.min = min;
            this.max = max;
        }

        @Override
        public T getMin() {
            return min;
        }

        @Override
        public T getMax() {
            return max;
        }
    }
}
