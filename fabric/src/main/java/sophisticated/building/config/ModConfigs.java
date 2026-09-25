package sophisticated.building.config;

import net.fabricmc.loader.api.FabricLoader;
import sophisticated.building.ClientConfig;
import sophisticated.building.CommonConfig;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;

import java.nio.file.Path;

/**
 * Loads the Fabric JSON configs from the Fabric config directory and applies the server values
 * synced to the client.
 */
public final class ModConfigs {

    private ModConfigs() {
    }

    public static void loadCommonAndServer() {
        load(spec(CommonConfig.spec));
        load(spec(ServerConfig.spec));
    }

    public static void loadServer() {
        load(spec(ServerConfig.spec));
    }

    public static void loadClient() {
        load(spec(ClientConfig.spec));
    }

    /** The Fabric spec behind one of the loader-neutral config classes. */
    public static ConfigSpec spec(Object spec) {
        return (ConfigSpec) spec;
    }

    /**
     * Applies the server's values on the client. Synced values the server does not send get their
     * default; server-only values (e.g. the whitelist) keep their local value.
     */
    public static void applyServerSync(String json) {
        ConfigSpec.LoadResult result = spec(ServerConfig.spec).loadSync(json);
        if (!result.parsed()) {
            SophisticatedBuilding.logger.error("Received invalid server config from the server, using defaults: {}", result.error());
        }
    }

    /** Restores the client's own server values after leaving a world or server. */
    public static void restoreLocalServer() {
        loadServer();
    }

    /** Writes a spec's current values to its JSON file. */
    public static void save(ConfigSpec spec) {
        ConfigFile.save(spec, FabricLoader.getInstance().getConfigDir(), SophisticatedBuilding.logger);
    }

    private static void load(ConfigSpec spec) {
        Path directory = FabricLoader.getInstance().getConfigDir();
        ConfigFile.load(spec, directory, SophisticatedBuilding.logger);
    }
}
