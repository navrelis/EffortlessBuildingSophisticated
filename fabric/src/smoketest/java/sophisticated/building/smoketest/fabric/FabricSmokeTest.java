package sophisticated.building.smoketest.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.SmokeServer;

/**
 * Fabric glue of the smoke harness (both sides): on the dedicated server of runSmokeServer {@link SmokeServer} runs the
 * server scenarios once the server has started (Fabric API 0.25 for 1.16.3 has no game test API).
 */
public final class FabricSmokeTest implements ModInitializer {
    @Override
    public void onInitialize() {
        SmokeServer.init();
        if (SmokeTest.isServerMode()) {
            ServerLifecycleEvents.SERVER_STARTED.register(SmokeServer::start);
            ServerTickEvents.END_SERVER_TICK.register(server -> SmokeServer.tick());
        }
    }
}
