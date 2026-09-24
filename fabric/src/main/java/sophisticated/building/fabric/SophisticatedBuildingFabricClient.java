package sophisticated.building.fabric;

import net.fabricmc.api.ClientModInitializer;

public final class SophisticatedBuildingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricBootstrap.initializeClient();
    }
}
