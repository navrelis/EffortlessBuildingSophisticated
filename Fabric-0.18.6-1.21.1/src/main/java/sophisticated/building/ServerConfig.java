package sophisticated.building;

import sophisticated.building.config.ConfigSpec;
import sophisticated.building.config.SimpleConfigValue;

import java.util.Arrays;
import java.util.List;

public class ServerConfig {
    private static final ConfigSpec.Builder builder = new ConfigSpec.Builder("server");
    public static final Validation validation = new Validation(builder);
    public static final Memory memory = new Memory(builder);
    public static final SurvivalBreaking survivalBreaking = new SurvivalBreaking(builder);
    public static final SurvivalReplace survivalReplace = new SurvivalReplace(builder);
    public static final ConfigSpec spec = builder.build();

    public static class Validation {
        public final SimpleConfigValue<Boolean> allowInSurvival;
        public final SimpleConfigValue<Boolean> useWhitelist;
        public final SimpleConfigValue<List<String>> whitelist;
        public final SimpleConfigValue<Integer> maxBlocksPlacedAtOnce;

        public Validation(ConfigSpec.Builder builder) {
            builder.push("Validation");

            allowInSurvival = builder
                    .comment("Allow use of the mod for players that are in survival mode. Otherwise, only creative mode players can use the mod.")
                    .define("allowInSurvival", true);

            useWhitelist = builder
                    .comment("Use a whitelist to determine which players can use the mod. If false, all players can use the mod.")
                    .define("useWhitelist", false);

            whitelist = builder
                    .comment("List of player names that can use the mod.")
                    .notSynced()
                    .defineList("whitelist", Arrays.asList("Player1", "Player2"));

            maxBlocksPlacedAtOnce = builder
                    .comment("Maximum number of blocks that can be placed at once. This is a last check. If you want the player to receive visual feedback instead of an error message, change values in the common config.")
                    .defineInRange("maxBlocksPlacedAtOnce", 10000, 1, 100000);

            builder.pop();
        }
    }

    public static class Memory {
        public final SimpleConfigValue<Integer> undoStackSize;

        public Memory(ConfigSpec.Builder builder) {
            builder.push("Memory");

            undoStackSize = builder
                    .comment("How many sets of blocks are remembered for the undo functionality, per player.")
                    .defineInRange("undoStackSize", 50, 10, 200);

            builder.pop();
        }
    }

    public static class SurvivalBreaking {
        public final SimpleConfigValue<Boolean> enabled;
        public final SimpleConfigValue<Boolean> stopBeforeToolBreaks;
        public final SimpleConfigValue<Integer> maxDelayTicks;
        public final SimpleConfigValue<Double> exhaustionPerBlock;

        public SurvivalBreaking(ConfigSpec.Builder builder) {
            builder.push("SurvivalBreaking");

            enabled = builder
                    .comment("Allow build-mode mass breaking for survival players (creative players can always use it).")
                    .define("enabled", true);

            stopBeforeToolBreaks = builder
                    .comment("Skip a candidate tool once it has 1 use left, so the mod never breaks your tool.")
                    .define("stopBeforeToolBreaks", true);

            maxDelayTicks = builder
                    .comment("Maximum mining delay (in ticks) for one survival break operation.")
                    .defineInRange("maxDelayTicks", 40, 0, 1200);

            exhaustionPerBlock = builder
                    .comment("Hunger exhaustion applied per block broken in survival, like vanilla mining.")
                    .defineInRange("exhaustionPerBlock", 0.005, 0.0, 1.0);

            builder.pop();
        }
    }

    public static class SurvivalReplace {
        public final SimpleConfigValue<Boolean> enabled;

        public SurvivalReplace(ConfigSpec.Builder builder) {
            builder.push("SurvivalReplace");

            enabled = builder
                    .comment("Allow survival players to use the replace modes of the radial menu (replace blocks and air / only blocks / filtered by offhand) and Quick Replace. Replaced blocks are mined like survival breaking: the right tool is used and loses durability, drops go to the inventory, protected and unbreakable blocks are skipped, and the mining delay applies.")
                    .define("enabled", false);

            builder.pop();
        }
    }
}
