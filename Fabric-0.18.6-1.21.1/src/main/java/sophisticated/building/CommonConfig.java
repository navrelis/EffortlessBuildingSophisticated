package sophisticated.building;

import sophisticated.building.config.ConfigSpec;
import sophisticated.building.config.SimpleConfigValue;

public class CommonConfig {
    private static final ConfigSpec.Builder builder = new ConfigSpec.Builder("common");
    public static final Reach reach = new Reach(builder);
    public static final MaxBlocksPlacedAtOnce maxBlocksPlacedAtOnce = new MaxBlocksPlacedAtOnce(builder);
    public static final MaxBlocksPerAxis maxBlocksPerAxis = new MaxBlocksPerAxis(builder);
    public static final MaxMirrorRadius maxMirrorRadius = new MaxMirrorRadius(builder);
    public static final ConfigSpec spec = builder.build();

    public static class Reach {
        public final SimpleConfigValue<Integer> creative;
        public final SimpleConfigValue<Integer> level0;
        public final SimpleConfigValue<Integer> level1;
        public final SimpleConfigValue<Integer> level2;
        public final SimpleConfigValue<Integer> level3;

        public Reach(ConfigSpec.Builder builder) {
            builder.push("Reach");

            creative = builder
                .comment("How far away the player can place and break blocks.")
                .defineInRange("reachCreative", 200, 0, 1000);

            level0 = builder
                .comment("Maximum reach in survival without upgrades",
                    "Consume Power Level upgrades to permanently increase this.")
                .defineInRange("reachLevel0", 0, 0, 1000);

            level1 = builder
                .defineInRange("reachLevel1", 8, 0, 1000);

            level2 = builder
                .defineInRange("reachLevel2", 16, 0, 1000);

            level3 = builder
                .defineInRange("reachLevel3", 32, 0, 1000);

            builder.pop();
        }
    }

    public static class MaxBlocksPlacedAtOnce {
        public final SimpleConfigValue<Integer> creative;
        public final SimpleConfigValue<Integer> level0;
        public final SimpleConfigValue<Integer> level1;
        public final SimpleConfigValue<Integer> level2;
        public final SimpleConfigValue<Integer> level3;

        public MaxBlocksPlacedAtOnce(ConfigSpec.Builder builder) {
            builder.push("MaxBlocksPlacedAtOnce");

            creative = builder
                .comment("How many blocks can be placed in one click.")
                .defineInRange("maxBlocksPlacedAtOnceCreative", 10000, 0, 100000);

            level0 = builder
                .comment("In survival without upgrades",
                        "Consume Power Level upgrades to permanently increase this.",
                        "Set to 0 to disable Sophisticated Building until the player has increased their Building Power Level.")
                .defineInRange("maxBlocksPlacedAtOnceLevel0", 128, 0, 100000);

            level1 = builder
                .defineInRange("maxBlocksPlacedAtOnceLevel1", 256, 0, 100000);

            level2 = builder
                .defineInRange("maxBlocksPlacedAtOnceLevel2", 512, 0, 100000);

            level3 = builder
                .defineInRange("maxBlocksPlacedAtOnceLevel3", 2048, 0, 100000);

            builder.pop();
        }
    }

    public static class MaxBlocksPerAxis {
        public final SimpleConfigValue<Integer> creative;
        public final SimpleConfigValue<Integer> level0;
        public final SimpleConfigValue<Integer> level1;
        public final SimpleConfigValue<Integer> level2;
        public final SimpleConfigValue<Integer> level3;

        public MaxBlocksPerAxis(ConfigSpec.Builder builder) {
            builder.push("MaxBlocksPerAxis");

            creative = builder
                .comment("How many blocks can be placed at once per axis when using build modes (e.g. walls).",
                        "Also affects the array modifier.")
                .defineInRange("maxBlocksPerAxisCreative", 1000, 0, 1000);

            level0 = builder
                .comment("In survival without upgrades",
                        "Consume Power Level upgrades to permanently increase this.")
                .defineInRange("maxBlocksPerAxisLevel0", 8, 0, 1000);

            level1 = builder
                .defineInRange("maxBlocksPerAxisLevel1", 16, 0, 1000);

            level2 = builder
                .defineInRange("maxBlocksPerAxisLevel2", 24, 0, 1000);

            level3 = builder
                .defineInRange("maxBlocksPerAxisLevel3", 32, 0, 1000);

            builder.pop();
        }
    }

    public static class MaxMirrorRadius {
        public final SimpleConfigValue<Integer> creative;
        public final SimpleConfigValue<Integer> level0;
        public final SimpleConfigValue<Integer> level1;
        public final SimpleConfigValue<Integer> level2;
        public final SimpleConfigValue<Integer> level3;

        public MaxMirrorRadius(ConfigSpec.Builder builder) {
            builder.push("MaxMirrorRadius");

            creative = builder
                .comment("The maximum (radial) mirror radius.")
                .defineInRange("maxMirrorRadiusCreative", 200, 0, 1000);

            level0 = builder
                .comment("Maximum reach in survival without upgrades",
                        "Consume Power Level upgrades upgrades to permanently increase this.")
                .defineInRange("maxMirrorRadiusLevel0", 16, 0, 1000);

            level1 = builder
                .defineInRange("maxMirrorRadiusLevel1", 32, 0, 1000);

            level2 = builder
                .defineInRange("maxMirrorRadiusLevel2", 48, 0, 1000);

            level3 = builder
                .defineInRange("maxMirrorRadiusLevel3", 64, 0, 1000);

            builder.pop();
        }
    }
}
