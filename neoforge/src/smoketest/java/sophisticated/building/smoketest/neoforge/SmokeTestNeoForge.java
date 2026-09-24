package sophisticated.building.smoketest.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.SmokeServer;

/**
 * NeoForge glue of the smoke harness (mod "sophisticatedbuilding_smoketest", only loaded by runSmokeClient /
 * runSmokeServer). The server scenarios are registered as game tests by {@link NeoForgeSmokeServerTests}'s
 * {@code @GameTestHolder}.
 */
@Mod(SmokeTest.MOD_ID)
public final class SmokeTestNeoForge {
    public SmokeTestNeoForge(Dist dist) {
        SmokeServer.init();
        if (dist.isClient() && SmokeTest.isClientMode()) {
            SmokeTestNeoForgeClient.init();
        }
    }
}
