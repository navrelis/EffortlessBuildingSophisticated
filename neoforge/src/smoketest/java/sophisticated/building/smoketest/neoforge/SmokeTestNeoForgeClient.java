package sophisticated.building.smoketest.neoforge;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import sophisticated.building.smoketest.client.SmokeClient;

/** Client half of the NeoForge glue: the end-of-tick hook and the harness start. */
final class SmokeTestNeoForgeClient {
    private SmokeTestNeoForgeClient() {
    }

    static void init() {
        NeoForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) SmokeClient.onClientTickEnd();
        });
        SmokeClient.init();
    }
}
