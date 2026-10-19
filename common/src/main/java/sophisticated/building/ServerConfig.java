package sophisticated.building;

import sophisticated.building.config.ConfigType;
import sophisticated.building.config.ConfigValue;
import sophisticated.building.config.IConfigBuilder;
import sophisticated.building.platform.Services;

import java.util.Arrays;
import java.util.List;

public class ServerConfig {
    private static final IConfigBuilder builder = Services.CONFIG.createBuilder(ConfigType.SERVER);
    public static final Validation validation = new Validation(builder);
    public static final Memory memory = new Memory(builder);
    public static final SurvivalBreaking survivalBreaking = new SurvivalBreaking(builder);
    public static final SurvivalReplace survivalReplace = new SurvivalReplace(builder);
    public static final BalancedBreaking balancedBreaking = new BalancedBreaking(builder);
    // The loader's spec object (Fabric ConfigSpec, NeoForge ModConfigSpec), registered by the loader project.
    public static final Object spec = builder.build();

    public static class Validation {
        public final ConfigValue<Boolean> allowInSurvival;
        public final ConfigValue<Boolean> useWhitelist;
        public final ConfigValue<List<String>> whitelist;
        public final ConfigValue<Integer> maxBlocksPlacedAtOnce;

        public Validation(IConfigBuilder builder) {
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
        public final ConfigValue<Integer> undoStackSize;

        public Memory(IConfigBuilder builder) {
            builder.push("Memory");

            undoStackSize = builder
                    .comment("How many sets of blocks are remembered for the undo functionality, per player.")
                    .worldRestart()
                    .defineInRange("undoStackSize", 50, 10, 200);

            builder.pop();
        }
    }

    public static class SurvivalBreaking {
        public final ConfigValue<Boolean> enabled;
        public final ConfigValue<Boolean> stopBeforeToolBreaks;
        public final ConfigValue<Integer> maxDelayTicks;
        public final ConfigValue<Double> exhaustionPerBlock;

        public SurvivalBreaking(IConfigBuilder builder) {
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
        public final ConfigValue<Boolean> enabled;

        public SurvivalReplace(IConfigBuilder builder) {
            builder.push("SurvivalReplace");

            enabled = builder
                    .comment("Allow survival players to use the replace modes of the radial menu (replace blocks and air / only blocks / filtered by offhand) and Quick Replace. Replaced blocks are mined like survival breaking: the right tool is used and loses durability, drops go to the inventory, protected and unbreakable blocks are skipped, and the mining delay applies.")
                    .define("enabled", false);

            builder.pop();
        }
    }

    public static class BalancedBreaking {
        public final ConfigValue<Boolean> enabled;
        public final ConfigValue<Integer> durabilityMultiplier;
        public final ConfigValue<Double> exhaustionPerBlock;
        public final ConfigValue<Integer> minFoodLevel;
        public final ConfigValue<Integer> cooldownBaseSeconds;
        public final ConfigValue<Integer> cooldownWindowMinutes;
        public final ConfigValue<Integer> cooldownFreeUses;
        public final ConfigValue<Integer> cooldownStepSeconds;
        public final ConfigValue<Integer> cooldownMaxSeconds;

        public BalancedBreaking(IConfigBuilder builder) {
            builder.comment("Balanced survival breaking: build-mode mass breaking and the mining of survival replace cost survival players more",
                            "tool durability and hunger, and each such operation starts an escalating cooldown. Operations that mine a single",
                            "block and creative players are never affected; placing is never affected.")
                    .push("BalancedBreaking");

            enabled = builder
                    .comment("Force balanced breaking for all players on this server.",
                            "In singleplayer (and on a LAN world you host) each player can also switch it on in the player settings (client config).")
                    .define("enabled", false);

            durabilityMultiplier = builder
                    .comment("Durability uses each mined block costs the tool (vanilla: 1). Unbreaking reduces it like vanilla.",
                            "With SurvivalBreaking.stopBeforeToolBreaks a tool is only used while it has more uses left than this.")
                    .defineInRange("durabilityMultiplier", 5, 1, 20);

            exhaustionPerBlock = builder
                    .comment("Hunger exhaustion per mined block, used instead of SurvivalBreaking.exhaustionPerBlock (4.0 exhaustion = 1 food point).")
                    .defineInRange("exhaustionPerBlock", 0.075, 0.0, 1.0);

            minFoodLevel = builder
                    .comment("A balanced operation is refused while the player's food level (0-20, two per shank) is below this. 0 = never refused.")
                    .defineInRange("minFoodLevel", 7, 0, 20);

            cooldownBaseSeconds = builder
                    .comment("Cooldown after a balanced operation, in real-time seconds. Real time: it also runs while the player is offline",
                            "or the server is stopped, and survives relog, restart, death and respawn.")
                    .defineInRange("cooldownBaseSeconds", 15, 0, 3600);

            cooldownWindowMinutes = builder
                    .comment("Rolling window, in minutes, in which the balanced operations of a player are counted for the escalation.")
                    .defineInRange("cooldownWindowMinutes", 30, 1, 1440);

            cooldownFreeUses = builder
                    .comment("Operations within the window that only get the base cooldown.")
                    .defineInRange("cooldownFreeUses", 10, 0, 1000);

            cooldownStepSeconds = builder
                    .comment("Seconds added to the cooldown for every operation within the window beyond the free ones.")
                    .defineInRange("cooldownStepSeconds", 5, 0, 3600);

            cooldownMaxSeconds = builder
                    .comment("Longest cooldown, in seconds (never shorter than the base cooldown).")
                    .defineInRange("cooldownMaxSeconds", 120, 0, 86400);

            builder.pop();
        }
    }
}
