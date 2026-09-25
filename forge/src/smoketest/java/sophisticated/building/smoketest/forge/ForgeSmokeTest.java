package sophisticated.building.smoketest.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.SmokeServer;

/**
 * Forge glue of the smoke harness (mod "sophisticatedbuilding_smoketest", only loaded by runSmokeClient /
 * runSmokeServer). On the dedicated server of runSmokeServer {@link SmokeServer} runs the server scenarios once the
 * server has started (Forge 1.16.3 has no game test server).
 */
@Mod(SmokeTest.MOD_ID)
public final class ForgeSmokeTest {
    public ForgeSmokeTest() {
        SmokeServer.init();
        if (SmokeTest.isServerMode()) {
            MinecraftForge.EVENT_BUS.addListener((FMLServerStartedEvent event) -> SmokeServer.start(event.getServer()));
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
                if (event.phase == TickEvent.Phase.END) SmokeServer.tick();
            });
        }
        if (FMLEnvironment.dist.isClient() && SmokeTest.isClientMode()) {
            ForgeSmokeTestClient.init();
        }
    }
}
