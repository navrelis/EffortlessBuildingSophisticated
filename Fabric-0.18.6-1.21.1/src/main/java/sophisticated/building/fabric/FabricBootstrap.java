package sophisticated.building.fabric;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.network.PacketHandler;
import sophisticated.building.network.PacketHandlerClient;

public final class FabricBootstrap {
    public static final String MODID = "sophisticatedbuilding";
    private static final Logger LOGGER = LogManager.getLogger(MODID);

    private FabricBootstrap() {
    }

    public static void initializeCommon() {
        LOGGER.info("Initializing Sophisticated Building for Fabric");
        SophisticatedBuilding.initializeCommon();
        PacketHandler.setupCommon();
        FabricCommonEvents.register();

        if (!CompatHelper.isCatnipLoaded()) {
            LOGGER.warn("Catnip/Create rendering stack is unavailable. Preview/outliner visuals will be limited unless Ponder/Flywheel are present.");
        }
        if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
            LOGGER.info("SophisticatedBackpacks is not loaded. Backpack upgrade integration is disabled.");
        }

        // Defer optional backpacks integration until lifecycle startup to avoid early classloading stalls.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> SophisticatedBuilding.registerBackpacksUpgradeContainers());
    }

    public static void initializeClient() {
        LOGGER.info("Initializing Sophisticated Building Fabric client hooks");
        PacketHandlerClient.setupClient();
        SophisticatedBuildingClient.initializeClient();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            SophisticatedBuilding.registerBackpacksUpgradeContainers();
            FabricClientEvents.registerOptionalIntegrations();
        });
    }
}
