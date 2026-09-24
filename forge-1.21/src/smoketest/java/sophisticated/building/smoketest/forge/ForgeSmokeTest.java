package sophisticated.building.smoketest.forge;

import net.minecraft.gametest.framework.GameTestServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.server.ServerLifecycleHooks;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.server.SmokeServer;

/**
 * Forge glue of the smoke harness (mod "sophisticatedbuilding_smoketest", only loaded by runSmokeClient /
 * runSmokeServer). The server scenarios are registered as game tests by {@link ForgeSmokeServerTests}'s
 * {@code @GameTestHolder}. Forge 51 constructs mods only through a no-argument constructor.
 * <p>
 * Forge 51's game test server never runs the server start hooks of the dedicated and integrated servers, so the
 * mod's SERVER config would stay unloaded ("Cannot get config value before config is loaded"). Before its first tick
 * the harness runs {@link ServerLifecycleHooks#handleServerAboutToStart} for it, as the dedicated server does.
 */
@Mod(SmokeTest.MOD_ID)
public final class ForgeSmokeTest {
    private static boolean serverPrepared;

    public ForgeSmokeTest() {
        SmokeServer.init();
        if (SmokeTest.isServerMode()) {
            MinecraftForge.EVENT_BUS.addListener(ForgeSmokeTest::onServerTickPre);
        }
        if (FMLEnvironment.dist.isClient() && SmokeTest.isClientMode()) {
            ForgeSmokeTestClient.init();
        }
    }

    private static void onServerTickPre(TickEvent.ServerTickEvent.Pre event) {
        if (serverPrepared || !(event.getServer() instanceof GameTestServer server)) return;
        serverPrepared = true;
        ServerLifecycleHooks.handleServerAboutToStart(server);
    }
}
