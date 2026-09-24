package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import sophisticated.building.SophisticatedBuilding;

/**
 * GUI render pipelines of the mod. Catnip drew its stencilled icons through the GL stencil buffer; Minecraft render
 * pipelines (1.21.5+) have no stencil state, so a stencilled icon is drawn as one quad of the icon texture whose opaque
 * texels show the vertex colours (the gradient) and whose transparent texels are discarded
 * ({@code assets/sophisticatedbuilding/shaders/core/gui_stencil_gradient.fsh}).
 */
public final class GuiPipelines {

	/** Quads (position, UV, colour) that show only their vertex colour, where the bound texture is not transparent. */
	public static final RenderPipeline STENCIL_GRADIENT = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
		.withLocation(Identifier.fromNamespaceAndPath(SophisticatedBuilding.MODID, "pipeline/gui_stencil_gradient"))
		.withFragmentShader(Identifier.fromNamespaceAndPath(SophisticatedBuilding.MODID, "core/gui_stencil_gradient"))
		.build();

	private GuiPipelines() {
	}
}
