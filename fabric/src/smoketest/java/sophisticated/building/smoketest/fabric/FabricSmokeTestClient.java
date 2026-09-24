package sophisticated.building.smoketest.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.client.SmokeClient;

/** Fabric client glue of the smoke harness: client init and the end-of-tick hook. */
public final class FabricSmokeTestClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!SmokeTest.isClientMode()) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> SmokeClient.onClientTickEnd());
        SmokeClient.init();
    }
}
