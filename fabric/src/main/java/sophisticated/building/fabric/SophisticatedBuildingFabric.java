package sophisticated.building.fabric;

import net.fabricmc.api.ModInitializer;

public final class SophisticatedBuildingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricBootstrap.initializeCommon();
    }
}
