package sophisticated.building.smoketest.server;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import sophisticated.building.smoketest.SmokeTest;
import sophisticated.building.smoketest.backpack.SmokeBackpacks;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The server smoke scenarios as game test functions (Minecraft 1.21.5+ game test framework). The loader glue registers
 * {@link #functions()} in the test function registry under {@link #functionKey}; the test instances that run them are
 * data: {@code data/sophisticatedbuilding/test_instance/<name>.json} (template {@code smoketest_empty}, one
 * {@code test_environment} per test so every test is its own batch and they run one after the other, as on 1.21.1).
 * The {@code sb_} instances live in {@code common/src/smoketestBackpacks}, so only loaders with Sophisticated Backpacks
 * run them. {@link SmokeServer} reports the instance {@code sophisticatedbuilding:sb_tier_cap} as {@code sb.tier_cap}.
 */
public final class SmokeServerTests {

    private SmokeServerTests() {
    }

    /** Test functions by name; the {@code sb_} ones only when this loader build has the Sophisticated Backpacks fixture. */
    public static Map<String, Consumer<GameTestHelper>> functions() {
        Map<String, Consumer<GameTestHelper>> functions = new LinkedHashMap<>();
        functions.put("server_place_line_survival", ServerScenarios::server_place_line_survival);
        functions.put("server_undo_redo", ServerScenarios::server_undo_redo);
        if (SmokeBackpacks.find().isPresent()) {
            functions.put("sb_upgrade_supplies_blocks", ServerScenarios::sb_upgrade_supplies_blocks);
            functions.put("sb_disabled_upgrade_ignored", ServerScenarios::sb_disabled_upgrade_ignored);
            functions.put("sb_tier_cap", ServerScenarios::sb_tier_cap);
            functions.put("sb_tool_swapper_tools", ServerScenarios::sb_tool_swapper_tools);
            functions.put("sb_worn_backpack_chest", ServerScenarios::sb_worn_backpack_chest);
            functions.put("sb_worn_backpack", ServerScenarios::sb_worn_backpack);
        }
        return functions;
    }

    /** {@code sophisticatedbuilding_smoketest:<name>}, the function the test instance JSON names. */
    public static ResourceKey<Consumer<GameTestHelper>> functionKey(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(SmokeTest.MOD_ID, name));
    }
}
