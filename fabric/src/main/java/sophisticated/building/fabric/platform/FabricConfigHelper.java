package sophisticated.building.fabric.platform;

import sophisticated.building.config.ConfigSpec;
import sophisticated.building.config.ConfigType;
import sophisticated.building.config.IConfigBuilder;
import sophisticated.building.platform.services.IConfigHelper;

import java.util.Locale;

/**
 * Fabric config backend: one JSON file per type, {@code config/sophisticatedbuilding-<type>.json}
 * (see {@link sophisticated.building.config.ModConfigs}).
 */
public final class FabricConfigHelper implements IConfigHelper {

    @Override
    public IConfigBuilder createBuilder(ConfigType type) {
        return new ConfigSpec.Builder(type.name().toLowerCase(Locale.ROOT));
    }
}
