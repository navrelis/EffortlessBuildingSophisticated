package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.create.AllSpecialTextures;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Render types of the outliner. Adapted from Catnip ({@code sophisticated.building.create.catnip.render.PonderRenderTypes},
 * MIT License, Copyright (c) 2022 The Create Team, see LICENSE_Ponder.txt); the fluid type is removed. The translucent
 * types use the entity translucent shader without depth writes, culled or not (Minecraft 1.21.5 render pipelines).
 */
public abstract class OutlineRenderTypes extends RenderType {

	private static final RenderType OUTLINE_SOLID =
		RenderType.create(createLayerName("outline_solid"), 256, false, false, RenderPipelines.ENTITY_SOLID, CompositeState.builder()
			.setTextureState(new TextureStateShard(AllSpecialTextures.BLANK.getLocation(), TriState.FALSE, false))
			.setLightmapState(LIGHTMAP)
			.setOverlayState(OVERLAY)
			.createCompositeState(false));

	private static final Function<Boolean, RenderPipeline> TRANSLUCENT_PIPELINE = Util.memoize(cull ->
		RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
			.withLocation(ResourceLocation.fromNamespaceAndPath(SophisticatedBuilding.MODID, "pipeline/outline_translucent" + (cull ? "_cull" : "")))
			.withShaderDefine("ALPHA_CUTOUT", 0.1F)
			.withSampler("Sampler1")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withCull(cull)
			.withDepthWrite(false)
			.build());

	private static final BiFunction<ResourceLocation, Boolean, RenderType> OUTLINE_TRANSLUCENT = Util.memoize((texture, cull) ->
		RenderType.create(createLayerName("outline_translucent" + (cull ? "_cull" : "")), 256, false, true, TRANSLUCENT_PIPELINE.apply(cull), CompositeState.builder()
			.setTextureState(new TextureStateShard(texture, TriState.FALSE, false))
			.setLightmapState(LIGHTMAP)
			.setOverlayState(OVERLAY)
			.createCompositeState(false)));

	public static RenderType outlineSolid() {
		return OUTLINE_SOLID;
	}

	public static RenderType outlineTranslucent(ResourceLocation texture, boolean cull) {
		return OUTLINE_TRANSLUCENT.apply(texture, cull);
	}

	private static String createLayerName(String name) {
		return SophisticatedBuilding.MODID + ":" + name;
	}

	private OutlineRenderTypes(String name, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
