package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.BindGroupLayouts;
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
 * since 1.21.11 a render type is a pipeline plus a {@link RenderSetup}; since 26.1 blending and depth writes are
 * the pipeline's colour target and depth stencil states; since 26.2 the depth buffer is reversed, a nearer fragment has
 * the greater depth, and render types have no buffer size).
 */
public final class OutlineRenderTypes {

	/**
	 * The entity solid pipeline with a blend function that keeps the source colour, which draws like no blending. Since
	 * 26.2 the game draws geometry without blending with the solid features, before the ghost blocks; with blending the
	 * outline edges stay in the translucent custom geometry, in the order of their submits (after the ghost blocks, as
	 * on earlier versions).
	 */
	private static final RenderPipeline SOLID_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
		.withLocation(Identifier.fromNamespaceAndPath(SophisticatedBuilding.MODID, "pipeline/outline_solid"))
		.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
		.withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ZERO)))
		.build();

	private static final RenderType OUTLINE_SOLID =
		RenderType.create(createLayerName("outline_solid"), RenderSetup.builder(SOLID_PIPELINE)
			.withTexture("Sampler0", AllSpecialTextures.BLANK.getLocation())
			.useLightmap()
			.useOverlay()
			.createRenderSetup());

	private static final Function<Boolean, RenderPipeline> TRANSLUCENT_PIPELINE = Util.memoize(cull ->
		RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(SophisticatedBuilding.MODID, "pipeline/outline_translucent" + (cull ? "_cull" : "")))
			.withShaderDefine("ALPHA_CUTOUT", 0.1F)
			.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withCull(cull)
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.build());

	private static final BiFunction<Identifier, Boolean, RenderType> OUTLINE_TRANSLUCENT = Util.memoize((texture, cull) ->
		RenderType.create(createLayerName("outline_translucent" + (cull ? "_cull" : "")), RenderSetup.builder(TRANSLUCENT_PIPELINE.apply(cull))
			.withTexture("Sampler0", texture)
			.useLightmap()
			.useOverlay()
			.sortOnUpload()
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
