package sophisticated.building.smoketest.fabric;

import net.fabricmc.api.ModInitializer;
import sophisticated.building.smoketest.server.SmokeServer;

/** Fabric glue of the smoke harness (both sides): starts the server smoke run when runSmokeServer launched the game. */
public final class FabricSmokeTest implements ModInitializer {
    @Override
    public void onInitialize() {
        SmokeServer.init();
    }
}
