package sophisticated.building.neoforge.platform;

import net.neoforged.neoforge.common.ModConfigSpec;
import sophisticated.building.config.ConfigType;
import sophisticated.building.config.ConfigValue;
import sophisticated.building.config.IConfigBuilder;
import sophisticated.building.platform.services.IConfigHelper;

import java.util.List;

/**
 * NeoForge config backend: the options become {@link ModConfigSpec} values (TOML files; the SERVER
 * config is per world). The specs are registered by {@code SophisticatedBuildingNeoForge}.
 */
public final class NeoForgeConfigHelper implements IConfigHelper {

    @Override
    public IConfigBuilder createBuilder(ConfigType type) {
        return new Builder(new ModConfigSpec.Builder());
    }

    private record Builder(ModConfigSpec.Builder builder) implements IConfigBuilder {

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

        /** Every NeoForge config value is only read on its own side; there is no sync to leave out. */
        @Override
        public IConfigBuilder notSynced() {
            return this;
        }

        @Override
        public ConfigValue<Boolean> define(String key, boolean defaultValue) {
            return builder.define(key, defaultValue)::get;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ConfigValue<List<String>> defineList(String key, List<String> defaultValue) {
            ModConfigSpec.ConfigValue<List<? extends String>> value = builder.defineList(key, defaultValue, o -> true);
            return () -> (List<String>) value.get();
        }

        @Override
        public ConfigValue<Integer> defineInRange(String key, int defaultValue, int min, int max) {
            return builder.defineInRange(key, defaultValue, min, max)::get;
        }

        @Override
        public ConfigValue<Double> defineInRange(String key, double defaultValue, double min, double max) {
            return builder.defineInRange(key, defaultValue, min, max)::get;
        }

        @Override
        public ModConfigSpec build() {
            return builder.build();
        }
    }
}
