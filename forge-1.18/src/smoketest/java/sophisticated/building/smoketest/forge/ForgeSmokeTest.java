package sophisticated.building.smoketest.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.SmokeServer;

/**
 * Forge glue of the smoke harness (mod "sophisticatedbuilding_smoketest", only loaded by runSmokeClient /
 * runSmokeServer). Forge 38 has no game test server: runSmokeServer starts a dedicated server, on which
 * {@link SmokeServerRunner} runs the server scenarios of {@link ForgeSmokeServerTests}.
 */
@Mod(SmokeTest.MOD_ID)
public final class ForgeSmokeTest {
    public ForgeSmokeTest() {
        SmokeServer.init();
        if (SmokeTest.isServerMode()) {
            MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> SmokeServerRunner.start(event.getServer()));
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
                if (event.phase == TickEvent.Phase.END) {
                    SmokeServerRunner.tick(net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer());
                }
            });
        }
        if (FMLEnvironment.dist.isClient() && SmokeTest.isClientMode()) {
            ForgeSmokeTestClient.init();
        }
    }
}
