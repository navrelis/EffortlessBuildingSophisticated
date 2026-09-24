package sophisticated.building.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import sophisticated.building.SophisticatedBuilding;

import java.util.Optional;

/**
 * Render types of the mirror lines and planes. Both are drawn without depth test, so they stay visible through blocks.
 * Minecraft 1.21.5 moved the GL state (shader, blending, depth, culling, write masks) into render pipelines; since
 * 1.21.11 a render type is a pipeline plus a {@link RenderSetup}, and the line width is a vertex attribute
 * ({@link #LINE_WIDTH}). Since 26.1 depth test and depth writes are one optional depth stencil state; without one the
 * pipeline neither tests nor writes depth. Since 26.2 render types have no buffer size (the level's submits share one
 * staged vertex buffer).
 */
public final class BuildRenderTypes {

	/** Width of the mirror lines in pixels; every vertex of {@link #LINES} carries it. */
	public static final float LINE_WIDTH = 2.0F;

	/** Lines of the vanilla line shader (camera-facing quads); every vertex needs the line direction as normal. */
	private static final RenderPipeline LINES_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(SophisticatedBuilding.MODID, "pipeline/lines"))
			.withDepthStencilState(Optional.empty())
			.build();

	/**
	 * Translucent planes as quads (position, colour; the snippet's format and topology), visible from both sides,
	 * without depth writes.
	 */
	private static final RenderPipeline PLANES_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(SophisticatedBuilding.MODID, "pipeline/planes"))
			.withDepthStencilState(Optional.empty())
			.withCull(false)
			.build();

	public static final RenderType LINES = RenderType.create("sb_lines", RenderSetup.builder(LINES_PIPELINE)
			.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			.createRenderSetup());

	public static final RenderType PLANES = RenderType.create("sb_planes", RenderSetup.builder(PLANES_PIPELINE)
			.setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			.createRenderSetup());

	private BuildRenderTypes() {
	}
}
