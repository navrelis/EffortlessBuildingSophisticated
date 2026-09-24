package sophisticated.building.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

public class BuildRenderTypes extends RenderType {
	public static final RenderType LINES;
	public static final RenderType PLANES;

	static {
		final LineStateShard LINE = new LineStateShard(OptionalDouble.of(2.0));
		final int INITIAL_BUFFER_SIZE = 128;
		RenderType.CompositeState renderState;

		//LINES
		renderState = CompositeState.builder()
				.setLineState(LINE)
				.setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
				.setLayeringState(VIEW_OFFSET_Z_LAYERING)
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setTextureState(RenderStateShard.NO_TEXTURE)
				.setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
				.setLightmapState(RenderStateShard.NO_LIGHTMAP)
				.setWriteMaskState(COLOR_DEPTH_WRITE)
				.setCullState(RenderStateShard.NO_CULL)
				.createCompositeState(false);
		LINES = RenderType.create("sb_lines",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.LINES, INITIAL_BUFFER_SIZE, false, false, renderState);

		//PLANES
		renderState = CompositeState.builder()
				.setLineState(LINE)
				.setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
				.setLayeringState(VIEW_OFFSET_Z_LAYERING)
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setTextureState(RenderStateShard.NO_TEXTURE)
				.setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
				.setLightmapState(RenderStateShard.NO_LIGHTMAP)
				.setWriteMaskState(COLOR_WRITE)
				.setCullState(RenderStateShard.NO_CULL)
				.createCompositeState(false);
		PLANES = RenderType.create("sb_planes",
			DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLE_STRIP, INITIAL_BUFFER_SIZE, false, false, renderState);
	}

	public BuildRenderTypes(String p_173178_, VertexFormat p_173179_, VertexFormat.Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
		super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
	}
}
