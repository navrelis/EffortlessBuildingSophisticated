package sophisticated.building.smoketest.neoforge;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import sophisticated.building.smoketest.client.SmokeClient;

/** Client half of the NeoForge glue: the end-of-tick hook and the harness start. */
final class SmokeTestNeoForgeClient {
    private SmokeTestNeoForgeClient() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> SmokeClient.onClientTickEnd());
        SmokeClient.init();
    }
}
