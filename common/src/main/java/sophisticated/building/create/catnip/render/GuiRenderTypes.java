package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;
import sophisticated.building.SophisticatedBuilding;

import java.util.function.Function;

/**
 * GUI render types of the mod. Catnip drew its stencilled icons through the GL stencil buffer; Minecraft 1.21.5 render
 * pipelines have no stencil state, so a stencilled icon is drawn as one quad of the icon texture whose opaque texels
 * show the vertex colours (the gradient) and whose transparent texels are discarded
 * ({@code assets/sophisticatedbuilding/shaders/core/gui_stencil_gradient.fsh}).
 */
public abstract class GuiRenderTypes extends RenderType {

	private static final RenderPipeline STENCIL_GRADIENT_PIPELINE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
		.withLocation(ResourceLocation.fromNamespaceAndPath(SophisticatedBuilding.MODID, "pipeline/gui_stencil_gradient"))
		.withFragmentShader(ResourceLocation.fromNamespaceAndPath(SophisticatedBuilding.MODID, "core/gui_stencil_gradient"))
		.build();

	private static final Function<ResourceLocation, RenderType> STENCIL_GRADIENT = Util.memoize(texture ->
		RenderType.create(SophisticatedBuilding.MODID + ":gui_stencil_gradient", 1536, STENCIL_GRADIENT_PIPELINE, CompositeState.builder()
			.setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
			.createCompositeState(false)));

	/** Quads of {@code texture} (position, UV, colour) that show only their vertex colour, where the texture is not transparent. */
	public static RenderType stencilGradient(ResourceLocation texture) {
		return STENCIL_GRADIENT.apply(texture);
	}

	private GuiRenderTypes(String name, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
