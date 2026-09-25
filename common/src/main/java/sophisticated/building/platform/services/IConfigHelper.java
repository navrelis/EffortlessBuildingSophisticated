package sophisticated.building.platform.services;

import sophisticated.building.config.ConfigType;
import sophisticated.building.config.IConfigBuilder;

/**
 * Creates the config builders backed by the loader's config system (Fabric: JSON files, NeoForge:
 * {@code ModConfigSpec} TOML files, Forge: {@code ForgeConfigSpec} TOML files).
 */
public interface IConfigHelper {

    IConfigBuilder createBuilder(ConfigType type);

    /**
     * Writes the current values of a config (the object {@link IConfigBuilder#build()} returned) to its file, e.g.
     * after {@link sophisticated.building.config.ConfigValue#set} from the settings screen.
     */
    void save(Object spec);
}
