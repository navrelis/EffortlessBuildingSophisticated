package sophisticated.building;

import net.neoforged.neoforge.common.ModConfigSpec;

import static net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import static net.neoforged.neoforge.common.ModConfigSpec.Builder;
import static net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import static net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Client-side configuration for Sophisticated Building.
 * These settings affect only the visual appearance and client-side behavior.
 */
public class ClientConfig {

    private static final Builder builder = new Builder();
    public static final Visuals visuals = new Visuals(builder);
    public static final Performance performance = new Performance(builder);
    public static final ModConfigSpec spec = builder.build();

    public static class Visuals {
        public final BooleanValue showBlockPreviews;
        public final BooleanValue onlyShowBlockPreviewsWhenBuilding;
        public final BooleanValue showMiniBlockPreview;
        public final IntValue maxBlockPreviews;
        public final IntValue appearAnimationLength;
        public final IntValue breakAnimationLength;
        public final DoubleValue previewScale;

        public Visuals(Builder builder) {
            builder.push("Visuals");

            showBlockPreviews = builder
                    .comment("Show previews of the blocks while placing them.",
                            "Disable for better performance on low-end systems.")
                    .define("showBlockPreviews", true);

            onlyShowBlockPreviewsWhenBuilding = builder
                .comment("Show block previews only when actively using a build mode.",
                        "Reduces visual clutter when not building.")
                .define("onlyShowBlockPreviewsWhenBuilding", true);

            showMiniBlockPreview = builder
                    .comment("Show a small transparent block inside each preview to highlight block rotation.",
                            "Disable if you prefer the classic outline-only preview or want to reduce on-screen effects.")
                    .define("showMiniBlockPreview", true);

            maxBlockPreviews = builder
                .comment("Don't show individual block previews when placing more than this many blocks.",
                         "The outline will always be rendered regardless of this setting.",
                         "Lower values improve performance for large builds.")
                .defineInRange("maxBlockPreviews", 4096, 0, 100000);

            appearAnimationLength = builder
                    .comment("How long it takes for a block to appear when placed in ticks.",
                            "Set to 0 to disable animation for better performance.",
                            "20 ticks = 1 second")
                    .defineInRange("appearAnimationLength", 5, 0, 100);

            breakAnimationLength = builder
                    .comment("How long the break animation is in ticks.",
                            "Set to 0 to disable animation for better performance.",
                            "20 ticks = 1 second")
                    .defineInRange("breakAnimationLength", 10, 0, 100);

            previewScale = builder
                    .comment("The scale of the ghost block previews.",
                            "1.0 is full size, 0.5 is half size, etc.",
                            "Smaller values reduce visual clutter.")
                    .defineInRange("previewScale", 0.25, 0.05, 1.0);

            builder.pop();
        }
    }
    
    public static class Performance {
        public final IntValue previewRenderDistance;
        public final BooleanValue enableUpdateThrottling;
        public final IntValue maxMiniBlockPreviews;

        public Performance(Builder builder) {
            builder.comment("Performance settings for Sophisticated Building.",
                           "Adjust these if experiencing lag or frame drops.")
                   .push("Performance");

            previewRenderDistance = builder
                    .comment("Maximum distance in blocks to render block previews.",
                            "Blocks beyond this distance won't show individual previews.",
                            "Lower values improve performance for large builds.")
                    .defineInRange("previewRenderDistance", 64, 16, 256);

            enableUpdateThrottling = builder
                    .comment("Enable update throttling to reduce CPU usage when idle.",
                            "When enabled, calculations are skipped if the player hasn't moved.",
                            "Disable if experiencing issues with preview updates.")
                    .define("enableUpdateThrottling", true);

            maxMiniBlockPreviews = builder
                    .comment("Maximum number of mini block previews to render.",
                            "Mini previews show block rotation inside the ghost outline.",
                            "Set to 0 to always show mini previews regardless of count (no limit).")
                    .defineInRange("maxMiniBlockPreviews", 4096, 0, 100000);

            builder.pop();
        }
    }
}
