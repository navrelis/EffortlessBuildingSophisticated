package sophisticated.building.smoketest.forge;

import net.minecraftforge.gametest.GameTestHolder;
import sophisticated.building.SophisticatedBuilding;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import sophisticated.building.smoketest.server.ServerScenarios;

/**
 * The server smoke scenarios as Forge game tests (runSmokeServer). No Sophisticated Backpacks on Forge 1.21.4, so no
 * sb_ tests.
 * Method names are the check names with the first '_' for '.'; each test has its own batch so they run one after
 * the other.
 */
@GameTestHolder(SophisticatedBuilding.MODID)
public final class ForgeSmokeServerTests {

    @GameTest(template = "sophisticatedbuilding:smoketest_empty", batch = "smoke_1", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void server_place_line_survival(GameTestHelper helper) {
        ServerScenarios.server_place_line_survival(helper);
    }

    @GameTest(template = "sophisticatedbuilding:smoketest_empty", batch = "smoke_2", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void server_undo_redo(GameTestHelper helper) {
        ServerScenarios.server_undo_redo(helper);
    }

    @GameTest(template = "sophisticatedbuilding:smoketest_empty", batch = "smoke_merge", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void server_merge_undo_refund(GameTestHelper helper) {
        ServerScenarios.server_merge_undo_refund(helper);
    }

    @GameTest(template = "sophisticatedbuilding:smoketest_empty", batch = "smoke_refused", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void server_refused_place_not_charged(GameTestHelper helper) {
        ServerScenarios.server_refused_place_not_charged(helper);
    }
}
