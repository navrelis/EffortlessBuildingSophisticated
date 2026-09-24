package sophisticated.building.smoketest.forge;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.SmokeServer;
import sophisticated.building.smoketest.server.SmokeServerTests;

import java.util.function.Consumer;

/**
 * Forge glue of the smoke harness (mod "sophisticatedbuilding_smoketest", only loaded by runSmokeClient /
 * runSmokeServer). Registers the server scenarios as game test functions; their test instances are data (see
 * {@link SmokeServerTests}); Forge 58's game test server runs them (Forge 55 had none).
 */
@Mod(SmokeTest.MOD_ID)
public final class ForgeSmokeTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, SmokeTest.MOD_ID);

    public ForgeSmokeTest(FMLJavaModLoadingContext context) {
        SmokeServerTests.functions().forEach((name, function) -> TEST_FUNCTIONS.register(name, () -> function));
        TEST_FUNCTIONS.register(context.getModBusGroup());
        SmokeServer.init();
        if (FMLEnvironment.dist.isClient() && SmokeTest.isClientMode()) {
            ForgeSmokeTestClient.init();
        }
    }
}
