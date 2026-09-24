package sophisticated.building.config;

import java.util.function.Supplier;

/**
 * One config option as seen by the loader-neutral code, backed by the loader's config value
 * (Fabric {@code SimpleConfigValue}, NeoForge {@code ModConfigSpec.ConfigValue}).
 */
@FunctionalInterface
public interface ConfigValue<T> extends Supplier<T> {

    @Override
    T get();
}
