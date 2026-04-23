package sophisticated.building;

import sophisticated.building.config.SimpleConfigValue;

public class CommonConfig {
    public static final Reach reach = new Reach();
    public static final MaxBlocksPlacedAtOnce maxBlocksPlacedAtOnce = new MaxBlocksPlacedAtOnce();
    public static final MaxBlocksPerAxis maxBlocksPerAxis = new MaxBlocksPerAxis();
    public static final MaxMirrorRadius maxMirrorRadius = new MaxMirrorRadius();

    // Kept as a compatibility placeholder while config registration is migrated to Fabric.
    public static final Object spec = new Object();

    public static class Reach {
        public final SimpleConfigValue<Integer> creative = new SimpleConfigValue<>(200);
        public final SimpleConfigValue<Integer> level0 = new SimpleConfigValue<>(0);
        public final SimpleConfigValue<Integer> level1 = new SimpleConfigValue<>(8);
        public final SimpleConfigValue<Integer> level2 = new SimpleConfigValue<>(16);
        public final SimpleConfigValue<Integer> level3 = new SimpleConfigValue<>(32);
    }

    public static class MaxBlocksPlacedAtOnce {
        public final SimpleConfigValue<Integer> creative = new SimpleConfigValue<>(10000);
        public final SimpleConfigValue<Integer> level0 = new SimpleConfigValue<>(128);
        public final SimpleConfigValue<Integer> level1 = new SimpleConfigValue<>(256);
        public final SimpleConfigValue<Integer> level2 = new SimpleConfigValue<>(512);
        public final SimpleConfigValue<Integer> level3 = new SimpleConfigValue<>(2048);
    }

    public static class MaxBlocksPerAxis {
        public final SimpleConfigValue<Integer> creative = new SimpleConfigValue<>(1000);
        public final SimpleConfigValue<Integer> level0 = new SimpleConfigValue<>(8);
        public final SimpleConfigValue<Integer> level1 = new SimpleConfigValue<>(16);
        public final SimpleConfigValue<Integer> level2 = new SimpleConfigValue<>(24);
        public final SimpleConfigValue<Integer> level3 = new SimpleConfigValue<>(32);
    }

    public static class MaxMirrorRadius {
        public final SimpleConfigValue<Integer> creative = new SimpleConfigValue<>(200);
        public final SimpleConfigValue<Integer> level0 = new SimpleConfigValue<>(16);
        public final SimpleConfigValue<Integer> level1 = new SimpleConfigValue<>(32);
        public final SimpleConfigValue<Integer> level2 = new SimpleConfigValue<>(48);
        public final SimpleConfigValue<Integer> level3 = new SimpleConfigValue<>(64);
    }
}
