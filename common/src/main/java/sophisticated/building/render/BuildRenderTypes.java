package sophisticated.building.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import sophisticated.building.SophisticatedBuilding;

import java.util.OptionalDouble;

/**
 * Render types of the mirror lines and planes. Both are drawn without depth test, so they stay visible through blocks.
 * Minecraft 1.21.5 moved the GL state (shader, blending, depth, culling, write masks) into render pipelines.
 */
public abstract class BuildRenderTypes extends RenderType {
	private static final int INITIAL_BUFFER_SIZE = 128;

	/** Lines of the vanilla line shader (camera-facing quads, 2 px wide); every vertex needs the line direction as normal. */
	private static final RenderPipeline LINES_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
			.withLocation(ResourceLocation.fromNamespaceAndPath(SophisticatedBuilding.MODID, "pipeline/lines"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.build();

	/** Translucent planes as triangle strips, visible from both sides, without depth writes. */
	private static final RenderPipeline PLANES_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(ResourceLocation.fromNamespaceAndPath(SophisticatedBuilding.MODID, "pipeline/planes"))
			.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP)
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withDepthWrite(false)
			.withCull(false)
			.build();

	public static final RenderType LINES = RenderType.create("sb_lines", INITIAL_BUFFER_SIZE, LINES_PIPELINE,
			CompositeState.builder()
					.setLineState(new LineStateShard(OptionalDouble.of(2.0)))
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.createCompositeState(false));

	public static final RenderType PLANES = RenderType.create("sb_planes", INITIAL_BUFFER_SIZE, PLANES_PIPELINE,
			CompositeState.builder()
					.setLayeringState(VIEW_OFFSET_Z_LAYERING)
					.createCompositeState(false));

	private BuildRenderTypes(String name, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
