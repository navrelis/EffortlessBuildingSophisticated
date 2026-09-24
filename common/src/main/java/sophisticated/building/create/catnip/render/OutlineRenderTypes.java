package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.create.AllSpecialTextures;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Render types of the outliner. Adapted from Catnip ({@code sophisticated.building.create.catnip.render.PonderRenderTypes},
 * MIT License, Copyright (c) 2022 The Create Team, see LICENSE_Ponder.txt); the fluid type is removed. The translucent
 * types use the entity translucent shader without depth writes, culled or not (Minecraft 1.21.5 render pipelines;
 * since 1.21.11 a render type is a pipeline plus a {@link RenderSetup}).
 */
public final class OutlineRenderTypes {

	private static final RenderType OUTLINE_SOLID =
		RenderType.create(createLayerName("outline_solid"), RenderSetup.builder(RenderPipelines.ENTITY_SOLID)
			.withTexture("Sampler0", AllSpecialTextures.BLANK.getLocation())
			.useLightmap()
			.useOverlay()
			.bufferSize(256)
			.createRenderSetup());

	private static final Function<Boolean, RenderPipeline> TRANSLUCENT_PIPELINE = Util.memoize(cull ->
		RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(SophisticatedBuilding.MODID, "pipeline/outline_translucent" + (cull ? "_cull" : "")))
			.withShaderDefine("ALPHA_CUTOUT", 0.1F)
			.withSampler("Sampler1")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withCull(cull)
			.withDepthWrite(false)
			.build());

	private static final BiFunction<Identifier, Boolean, RenderType> OUTLINE_TRANSLUCENT = Util.memoize((texture, cull) ->
		RenderType.create(createLayerName("outline_translucent" + (cull ? "_cull" : "")), RenderSetup.builder(TRANSLUCENT_PIPELINE.apply(cull))
			.withTexture("Sampler0", texture)
			.useLightmap()
			.useOverlay()
			.sortOnUpload()
			.bufferSize(256)
			.createRenderSetup()));

	public static RenderType outlineSolid() {
		return OUTLINE_SOLID;
	}

	public static RenderType outlineTranslucent(Identifier texture, boolean cull) {
		return OUTLINE_TRANSLUCENT.apply(texture, cull);
	}

	private static String createLayerName(String name) {
		return SophisticatedBuilding.MODID + ":" + name;
	}

	private OutlineRenderTypes() {
	}
}
