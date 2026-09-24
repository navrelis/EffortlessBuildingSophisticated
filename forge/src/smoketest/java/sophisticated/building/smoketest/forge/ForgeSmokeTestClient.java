package sophisticated.building.smoketest.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import sophisticated.building.smoketest.client.SmokeClient;

/** Client half of the Forge glue: the end-of-tick hook and the harness start. */
final class ForgeSmokeTestClient {
    private ForgeSmokeTestClient() {
    }

    static void init() {
        MinecraftForge.EVENT_BUS.addListener(ForgeSmokeTestClient::onClientTickEnd);
        SmokeClient.init();
    }

    private static void onClientTickEnd(TickEvent.ClientTickEvent.Post event) {
        SmokeClient.onClientTickEnd();
    }
}
