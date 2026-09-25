package sophisticated.building;

import sophisticated.building.config.ConfigType;
import sophisticated.building.config.ConfigValue;
import sophisticated.building.config.IConfigBuilder;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.platform.Services;
import sophisticated.building.utilities.SyncedValues;

import javax.annotation.Nullable;
import java.util.List;

public class CommonConfig {
    private static final IConfigBuilder builder = Services.CONFIG.createBuilder(ConfigType.COMMON);
    public static final Reach reach = new Reach(builder);
    public static final MaxBlocksPlacedAtOnce maxBlocksPlacedAtOnce = new MaxBlocksPlacedAtOnce(builder);
    public static final MaxBlocksPerAxis maxBlocksPerAxis = new MaxBlocksPerAxis(builder);
    public static final MaxMirrorRadius maxMirrorRadius = new MaxMirrorRadius(builder);
    // The loader's spec object (Fabric ConfigSpec, NeoForge ModConfigSpec), registered by the loader project.
    public static final Object spec = builder.build();

    // The limits the client uses to build sets, in the order the server sends them (CommonConfigSyncPacket)
    private static final List<ConfigValue<Integer>> SYNCED = List.of(
            reach.creative, reach.level0, reach.level1, reach.level2, reach.level3,
            maxBlocksPlacedAtOnce.creative, maxBlocksPlacedAtOnce.level0, maxBlocksPlacedAtOnce.level1, maxBlocksPlacedAtOnce.level2, maxBlocksPlacedAtOnce.level3,
            maxBlocksPerAxis.creative, maxBlocksPerAxis.level0, maxBlocksPerAxis.level1, maxBlocksPerAxis.level2, maxBlocksPerAxis.level3,
            maxMirrorRadius.creative, maxMirrorRadius.level0, maxMirrorRadius.level1, maxMirrorRadius.level2, maxMirrorRadius.level3);
    // Client: the server's values while connected (the common config is not synced by the loaders)
    public static final SyncedValues SERVER_VALUES = new SyncedValues();

    /** This side's values of the synced limits, in their order. */
    public static int[] syncedValues() {
        int[] values = new int[SYNCED.size()];
        for (int i = 0; i < values.length; i++) values[i] = SYNCED.get(i).get();
        return values;
    }

    /**
     * A limit of the power levels for this player: on the client the server's value while connected, else this side's
     * config (the server always uses its own).
     */
    public static int value(@Nullable Player player, ConfigValue<Integer> value) {
        boolean clientSide = player != null && player.level().isClientSide();
        return SERVER_VALUES.pick(clientSide, SYNCED.indexOf(value), value.get());
    }

    public static class Reach {
        public final ConfigValue<Integer> creative;
        public final ConfigValue<Integer> level0;
        public final ConfigValue<Integer> level1;
        public final ConfigValue<Integer> level2;
        public final ConfigValue<Integer> level3;

        public Reach(IConfigBuilder builder) {
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
        public final ConfigValue<Integer> creative;
        public final ConfigValue<Integer> level0;
        public final ConfigValue<Integer> level1;
        public final ConfigValue<Integer> level2;
        public final ConfigValue<Integer> level3;

        public MaxBlocksPlacedAtOnce(IConfigBuilder builder) {
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
        public final ConfigValue<Integer> creative;
        public final ConfigValue<Integer> level0;
        public final ConfigValue<Integer> level1;
        public final ConfigValue<Integer> level2;
        public final ConfigValue<Integer> level3;

        public MaxBlocksPerAxis(IConfigBuilder builder) {
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
        public final ConfigValue<Integer> creative;
        public final ConfigValue<Integer> level0;
        public final ConfigValue<Integer> level1;
        public final ConfigValue<Integer> level2;
        public final ConfigValue<Integer> level3;

        public MaxMirrorRadius(IConfigBuilder builder) {
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
