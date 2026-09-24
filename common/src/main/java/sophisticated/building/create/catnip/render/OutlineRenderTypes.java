package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.create.AllSpecialTextures;

import java.util.function.BiFunction;

/**
 * Render types of the outliner. Adapted from Catnip ({@code sophisticated.building.create.catnip.render.PonderRenderTypes},
 * MIT License, Copyright (c) 2022 The Create Team, see LICENSE_Ponder.txt); the fluid type is removed.
 */
public abstract class OutlineRenderTypes extends RenderType {

	private static final RenderType OUTLINE_SOLID =
		RenderType.create(createLayerName("outline_solid"), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, false, CompositeState.builder()
			.setShaderState(RENDERTYPE_ENTITY_SOLID_SHADER)
			.setTextureState(new TextureStateShard(AllSpecialTextures.BLANK.getLocation(), false, false))
			.setCullState(CULL)
			.setLightmapState(LIGHTMAP)
			.setOverlayState(OVERLAY)
			.createCompositeState(false));

	private static final BiFunction<ResourceLocation, Boolean, RenderType> OUTLINE_TRANSLUCENT = Util.memoize((texture, cull) ->
		RenderType.create(createLayerName("outline_translucent" + (cull ? "_cull" : "")), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
			.setShaderState(cull ? RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER : RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
			.setTextureState(new TextureStateShard(texture, false, false))
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setCullState(cull ? CULL : NO_CULL)
			.setLightmapState(LIGHTMAP)
			.setOverlayState(OVERLAY)
			.setWriteMaskState(COLOR_WRITE)
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

	private OutlineRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
