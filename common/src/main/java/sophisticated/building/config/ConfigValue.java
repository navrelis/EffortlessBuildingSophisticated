package sophisticated.building.config;

import java.util.function.Supplier;

/**
 * One config option as seen by the loader-neutral code, backed by the loader's config value
 * (Fabric {@code SimpleConfigValue}, NeoForge {@code ModConfigSpec.ConfigValue}, Forge
 * {@code ForgeConfigSpec.ConfigValue}).
 */
public interface ConfigValue<T> extends Supplier<T> {

    @Override
    T get();

    /**
     * Changes the value in memory; the code reading it sees the new value at once. Written to the
     * config file only by {@link sophisticated.building.platform.services.IConfigHelper#save(Object)}.
     */
    void set(T value);

    T getDefault();
}
