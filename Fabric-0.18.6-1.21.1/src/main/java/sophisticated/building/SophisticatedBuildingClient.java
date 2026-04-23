package sophisticated.building;

import sophisticated.building.buildmode.BuildModes;
import sophisticated.building.buildmodifier.BuildModifiers;
import sophisticated.building.fabric.FabricClientEvents;
import sophisticated.building.render.BlockPreviews;
import sophisticated.building.systems.BuildSettings;
import sophisticated.building.systems.BuilderChain;
import sophisticated.building.systems.BuilderFilter;
import sophisticated.building.systems.ItemUsageTracker;

public class SophisticatedBuildingClient {

    public static final BuilderChain BUILDER_CHAIN = new BuilderChain();
    public static final BuildModes BUILD_MODES = new BuildModes();
    public static final BuildModifiers BUILD_MODIFIERS = new BuildModifiers();
    public static final BuildSettings BUILD_SETTINGS = new BuildSettings();
    public static final BlockPreviews BLOCK_PREVIEWS = new BlockPreviews();
    public static final BuilderFilter BUILDER_FILTER = new BuilderFilter();
    public static final ItemUsageTracker ITEM_USAGE_TRACKER = new ItemUsageTracker();

    public static void initializeClient() {
        FabricClientEvents.register();
    }
}
