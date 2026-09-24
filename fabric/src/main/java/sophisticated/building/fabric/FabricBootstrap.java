package sophisticated.building.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.config.ModConfigs;

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

        if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
            LOGGER.info("SophisticatedBackpacks is not loaded. Backpack upgrade integration is disabled.");
        }

        // Defer optional backpacks integration until lifecycle startup to avoid early classloading stalls.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SophisticatedBuilding.registerBackpacksUpgradeContainers());
    }

    public static void initializeClient() {
        LOGGER.info("Initializing Sophisticated Building Fabric client hooks");
        ModConfigs.loadClient();
        FabricClientNetworking.setupClient();
        FabricClientEvents.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            SophisticatedBuilding.registerBackpacksUpgradeContainers();
            FabricClientEvents.registerOptionalIntegrations();
        });
    }
}
