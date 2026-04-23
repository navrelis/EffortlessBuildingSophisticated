package sophisticated.building;

import sophisticated.building.config.SimpleConfigValue;

public class ClientConfig {
    public static final Visuals visuals = new Visuals();
    public static final Performance performance = new Performance();

    // Kept as a compatibility placeholder while config registration is migrated to Fabric.
    public static final Object spec = new Object();

    public static class Visuals {
        public final SimpleConfigValue<Boolean> showBlockPreviews = new SimpleConfigValue<>(true);
        public final SimpleConfigValue<Boolean> onlyShowBlockPreviewsWhenBuilding = new SimpleConfigValue<>(true);
        public final SimpleConfigValue<Boolean> showMiniBlockPreview = new SimpleConfigValue<>(true);
        public final SimpleConfigValue<Integer> maxBlockPreviews = new SimpleConfigValue<>(4096);
        public final SimpleConfigValue<Integer> appearAnimationLength = new SimpleConfigValue<>(5);
        public final SimpleConfigValue<Integer> breakAnimationLength = new SimpleConfigValue<>(10);
        public final SimpleConfigValue<Double> previewScale = new SimpleConfigValue<>(0.25);
    }

    public static class Performance {
        public final SimpleConfigValue<Integer> previewRenderDistance = new SimpleConfigValue<>(64);
        public final SimpleConfigValue<Boolean> enableUpdateThrottling = new SimpleConfigValue<>(true);
        public final SimpleConfigValue<Integer> maxMiniBlockPreviews = new SimpleConfigValue<>(4096);
    }
}
