package sophisticated.building.smoketest.fabric;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import sophisticated.building.smoketest.server.ServerScenarios;

/**
 * The server smoke scenarios as Fabric game tests (runSmokeServer). Method names are the check names with the first
 * '_' for '.'; each test has its own batch so they run one after the other.
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

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_3", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void sb_upgrade_supplies_blocks(GameTestHelper helper) {
        ServerScenarios.sb_upgrade_supplies_blocks(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_4", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void sb_disabled_upgrade_ignored(GameTestHelper helper) {
        ServerScenarios.sb_disabled_upgrade_ignored(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_5", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void sb_tier_cap(GameTestHelper helper) {
        ServerScenarios.sb_tier_cap(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_6", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void sb_tool_swapper_tools(GameTestHelper helper) {
        ServerScenarios.sb_tool_swapper_tools(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_7", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void sb_worn_backpack_chest(GameTestHelper helper) {
        ServerScenarios.sb_worn_backpack_chest(helper);
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "smoke_8", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public void sb_worn_backpack(GameTestHelper helper) {
        ServerScenarios.sb_worn_backpack(helper);
    }
}
