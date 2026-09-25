package sophisticated.building.smoketest.forge;

import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import sophisticated.building.SophisticatedBuilding;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import sophisticated.building.smoketest.server.ServerScenarios;

/**
 * The server smoke scenarios as Forge game tests (runSmokeServer), in the empty template
 * data/sophisticatedbuilding/structures/smoketest_empty.nbt.
 * Method names are the check names with the first '_' for '.'; each test has its own batch so they run one after
 * the other.
 */
@GameTestHolder(SophisticatedBuilding.MODID)
@PrefixGameTestTemplate(false)
public final class ForgeSmokeServerTests {

    @GameTest(template = "smoketest_empty", batch = "smoke_1", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void server_place_line_survival(GameTestHelper helper) {
        ServerScenarios.server_place_line_survival(helper);
    }

    @GameTest(template = "smoketest_empty", batch = "smoke_2", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void server_undo_redo(GameTestHelper helper) {
        ServerScenarios.server_undo_redo(helper);
    }

    @GameTest(template = "smoketest_empty", batch = "smoke_3", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void sb_upgrade_supplies_blocks(GameTestHelper helper) {
        ServerScenarios.sb_upgrade_supplies_blocks(helper);
    }

    @GameTest(template = "smoketest_empty", batch = "smoke_4", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void sb_disabled_upgrade_ignored(GameTestHelper helper) {
        ServerScenarios.sb_disabled_upgrade_ignored(helper);
    }

    @GameTest(template = "smoketest_empty", batch = "smoke_5", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void sb_tier_cap(GameTestHelper helper) {
        ServerScenarios.sb_tier_cap(helper);
    }

    @GameTest(template = "smoketest_empty", batch = "smoke_6", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void sb_tool_swapper_tools(GameTestHelper helper) {
        ServerScenarios.sb_tool_swapper_tools(helper);
    }

    @GameTest(template = "smoketest_empty", batch = "smoke_7", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void sb_worn_backpack_chest(GameTestHelper helper) {
        ServerScenarios.sb_worn_backpack_chest(helper);
    }

    @GameTest(template = "smoketest_empty", batch = "smoke_8", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void sb_worn_backpack(GameTestHelper helper) {
        ServerScenarios.sb_worn_backpack(helper);
    }

    @GameTest(template = "smoketest_empty", batch = "smoke_merge", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void server_merge_undo_refund(GameTestHelper helper) {
        ServerScenarios.server_merge_undo_refund(helper);
    }

    @GameTest(template = "smoketest_empty", batch = "smoke_refused", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void server_refused_place_not_charged(GameTestHelper helper) {
        ServerScenarios.server_refused_place_not_charged(helper);
    }

    @GameTest(template = "smoketest_empty", batch = "smoke_limits", timeoutTicks = ServerScenarios.TIMEOUT_TICKS)
    public static void server_request_limits(GameTestHelper helper) {
        ServerScenarios.server_request_limits(helper);
    }
}
