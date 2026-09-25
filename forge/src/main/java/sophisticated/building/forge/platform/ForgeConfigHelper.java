package sophisticated.building.forge.platform;

import net.minecraftforge.common.ForgeConfigSpec;
import sophisticated.building.config.ConfigType;
import sophisticated.building.config.ConfigValue;
import sophisticated.building.config.IConfigBuilder;
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

    private static final class Builder implements IConfigBuilder {
        private final ForgeConfigSpec.Builder builder;

        public Builder(ForgeConfigSpec.Builder builder) {
            this.builder = builder;
        }

        public ForgeConfigSpec.Builder builder() {
            return builder;
        }

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
            return builder.define(key, defaultValue)::get;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ConfigValue<List<String>> defineList(String key, List<String> defaultValue) {
            ForgeConfigSpec.ConfigValue<List<? extends String>> value = builder.defineList(key, defaultValue, o -> true);
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
        public ForgeConfigSpec build() {
            return builder.build();
        }
    }
}
