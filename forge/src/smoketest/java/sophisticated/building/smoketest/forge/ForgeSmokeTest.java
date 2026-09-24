package sophisticated.building.smoketest.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.SmokeServer;

/**
 * Forge glue of the smoke harness (mod "sophisticatedbuilding_smoketest", only loaded by runSmokeClient /
 * runSmokeServer). On the dedicated server of runSmokeServer {@link ForgeSmokeServerTests} runs the server scenarios as
 * game tests.
 */
@Mod(SmokeTest.MOD_ID)
public final class ForgeSmokeTest {
    public ForgeSmokeTest() {
        SmokeServer.init();
        if (SmokeTest.isServerMode()) {
            ForgeSmokeServerTests.init();
        }
        if (FMLEnvironment.dist.isClient() && SmokeTest.isClientMode()) {
            ForgeSmokeTestClient.init();
        }
    }
}
