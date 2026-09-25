package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.create.AllSpecialTextures;

import org.lwjgl.opengl.GL11;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Render types of the outliner. Adapted from Catnip ({@code sophisticated.building.create.catnip.render.PonderRenderTypes},
 * MIT License, Copyright (c) 2022 The Create Team, see LICENSE_Ponder.txt); the fluid type is removed. Minecraft 1.16.5
 * has no core shaders: the states of its entity solid / entity translucent (cull) render types stand in for the
 * entity shaders of Minecraft 1.17+.
 */
public abstract class OutlineRenderTypes extends RenderType {

	private static final RenderType OUTLINE_SOLID =
		RenderType.create(createLayerName("outline_solid"), DefaultVertexFormat.NEW_ENTITY, GL11.GL_QUADS, 256, false, false, CompositeState.builder()
			.setTextureState(new TextureStateShard(AllSpecialTextures.BLANK.getLocation(), false, false))
			.setDiffuseLightingState(DIFFUSE_LIGHTING)
			.setCullState(CULL)
			.setLightmapState(LIGHTMAP)
			.setOverlayState(OVERLAY)
			.createCompositeState(false));

	private static final Map<String, RenderType> OUTLINE_TRANSLUCENT = new ConcurrentHashMap<>();

	public static RenderType outlineSolid() {
		return OUTLINE_SOLID;
	}

	public static RenderType outlineTranslucent(ResourceLocation texture, boolean cull) {
		return OUTLINE_TRANSLUCENT.computeIfAbsent(texture + (cull ? "#cull" : ""), key ->
			RenderType.create(createLayerName("outline_translucent" + (cull ? "_cull" : "")), DefaultVertexFormat.NEW_ENTITY, GL11.GL_QUADS, 256, false, true, CompositeState.builder()
				.setTextureState(new TextureStateShard(texture, false, false))
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setDiffuseLightingState(DIFFUSE_LIGHTING)
				.setAlphaState(DEFAULT_ALPHA)
				.setCullState(cull ? CULL : NO_CULL)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setWriteMaskState(COLOR_WRITE)
				.createCompositeState(false)));
	}

	private static String createLayerName(String name) {
		return SophisticatedBuilding.MODID + ":" + name;
	}

	private OutlineRenderTypes(String name, VertexFormat format, int mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
	}
}
