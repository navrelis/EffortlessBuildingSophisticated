package sophisticated.building.smoketest.fabric;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import sophisticated.building.smoketest.server.ServerScenarios;

/**
 * The server smoke scenarios as Fabric game tests (runSmokeServer). No Sophisticated Backpacks on Fabric 1.17.1, so no
 * sb_ tests.
 * Method names are the check names with the first '_' for '.'; each test has its own batch so they run one after
 * the other.
 */
public final class FabricSmokeServerTests implements FabricGameTest {

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_1", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void server_place_line_survival(GameTestHelper helper) {
        ServerScenarios.server_place_line_survival(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_2", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void server_undo_redo(GameTestHelper helper) {
        ServerScenarios.server_undo_redo(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_merge", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void server_merge_undo_refund(GameTestHelper helper) {
        ServerScenarios.server_merge_undo_refund(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_refused", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void server_refused_place_not_charged(GameTestHelper helper) {
        ServerScenarios.server_refused_place_not_charged(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_limits", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void server_request_limits(GameTestHelper helper) {
        ServerScenarios.server_request_limits(helper);
    }
}
