package sophisticated.building.platform.services;

import sophisticated.building.config.ConfigType;
import sophisticated.building.config.IConfigBuilder;

/**
 * Creates the config builders backed by the loader's config system (Fabric: JSON files, NeoForge:
 * {@code ModConfigSpec} TOML files).
 */
public interface IConfigHelper {

    IConfigBuilder createBuilder(ConfigType type);
}
