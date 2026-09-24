package sophisticated.building.smoketest.neoforge;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.SmokeServer;
import sophisticated.building.smoketest.server.SmokeServerTests;

import java.util.function.Consumer;

/**
 * NeoForge glue of the smoke harness (mod "sophisticatedbuilding_smoketest", only loaded by runSmokeClient /
 * runSmokeServer). Registers the server scenarios as game test functions; their test instances are data (see
 * {@link SmokeServerTests}).
 */
@Mod(SmokeTest.MOD_ID)
public final class SmokeTestNeoForge {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, SmokeTest.MOD_ID);

    public SmokeTestNeoForge(IEventBus modEventBus, Dist dist) {
        SmokeServerTests.functions().forEach((name, function) -> TEST_FUNCTIONS.register(name, () -> function));
        TEST_FUNCTIONS.register(modEventBus);
        SmokeServer.init();
        if (dist.isClient() && SmokeTest.isClientMode()) {
            SmokeTestNeoForgeClient.init();
        }
    }
}
