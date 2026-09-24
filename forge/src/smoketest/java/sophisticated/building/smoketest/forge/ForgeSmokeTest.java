package sophisticated.building.smoketest.forge;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.SmokeServer;
import sophisticated.building.smoketest.server.SmokeServerRunner;
import sophisticated.building.smoketest.server.SmokeServerTests;

import java.util.function.Consumer;

/**
 * Forge glue of the smoke harness (mod "sophisticatedbuilding_smoketest", only loaded by runSmokeClient /
 * runSmokeServer). Registers the server scenarios as game test functions; their test instances are data (see
 * {@link SmokeServerTests}). Forge 55's game test launch target starts a plain dedicated server that runs no tests, so
 * {@link SmokeServerRunner} runs them there.
 */
@Mod(SmokeTest.MOD_ID)
public final class ForgeSmokeTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, SmokeTest.MOD_ID);

    public ForgeSmokeTest(FMLJavaModLoadingContext context) {
        SmokeServerTests.functions().forEach((name, function) -> TEST_FUNCTIONS.register(name, () -> function));
        TEST_FUNCTIONS.register(context.getModEventBus());
        SmokeServer.init();
        if (SmokeTest.isServerMode()) {
            MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> SmokeServerRunner.start(event.getServer()));
            MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post event) -> SmokeServerRunner.tick(event.getServer()));
        }
        if (FMLEnvironment.dist.isClient() && SmokeTest.isClientMode()) {
            ForgeSmokeTestClient.init();
        }
    }
}
