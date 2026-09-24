package sophisticated.building.smoketest.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.SmokeServer;

/**
 * Forge glue of the smoke harness (mod "sophisticatedbuilding_smoketest", only loaded by runSmokeClient /
 * runSmokeServer). The server scenarios are registered as game tests by {@link ForgeSmokeServerTests}'s
 * {@code @GameTestHolder}.
 */
@Mod(SmokeTest.MOD_ID)
public final class ForgeSmokeTest {
    public ForgeSmokeTest(FMLJavaModLoadingContext context) {
        SmokeServer.init();
        if (FMLEnvironment.dist.isClient() && SmokeTest.isClientMode()) {
            ForgeSmokeTestClient.init();
        }
    }
}
