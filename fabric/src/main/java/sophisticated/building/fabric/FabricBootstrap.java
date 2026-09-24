package sophisticated.building.fabric;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.config.ModConfigs;

/**
 * Sophisticated Backpacks has no Fabric build for this Minecraft version, so this loader ships no
 * backpack integration (the Building Upgrades stay placeholder items).
 */
public final class FabricBootstrap {
    private static final Logger LOGGER = LogManager.getLogger(SophisticatedBuilding.MODID);

    private FabricBootstrap() {
    }

    public static void initializeCommon() {
        LOGGER.info("Initializing Sophisticated Building for Fabric");
        ModConfigs.loadCommonAndServer();
        // Re-read so edits to the server config between singleplayer worlds take effect.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> ModConfigs.loadServer());
        SophisticatedBuilding.init();
        FabricNetworking.setupCommon();
        FabricCommonEvents.register();
    }

    public static void initializeClient() {
        LOGGER.info("Initializing Sophisticated Building Fabric client hooks");
        ModConfigs.loadClient();
        FabricClientNetworking.setupClient();
        FabricClientEvents.register();
    }
}
