package sophisticated.building.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import sophisticated.building.mixin.GuiGraphicsAccessor;

import javax.annotation.Nullable;

/**
 * Free-form GUI quads. Minecraft 1.21.6 replaced the immediate GUI buffers with render states, and {@link GuiGraphicsExtractor}
 * only submits axis-aligned rectangles; this element submits any number of quads (four vertices each, in GUI
 * coordinates, transformed by the pose at the time of {@link #submit}). The GUI render state layers it like any vanilla
 * element: above what was submitted before it where the bounds intersect. The quads are not clipped by an enabled
 * scissor (none of the mod's free-form quads is drawn inside a scrolled area).
 */
public final class GuiQuads implements GuiElementRenderState {

	private final RenderPipeline pipeline;
	private final TextureSetup textureSetup;
	private final boolean textured;
	private final Matrix3x2f pose;
	/** x, y, u, v per vertex (u and v unused without a texture). */
	private final FloatArrayList vertices = new FloatArrayList();
	private final IntArrayList colors = new IntArrayList();
	@Nullable
	private ScreenRectangle bounds;

	private GuiQuads(GuiGraphicsExtractor graphics, RenderPipeline pipeline, TextureSetup textureSetup, boolean textured) {
		this.pipeline = pipeline;
		this.textureSetup = textureSetup;
		this.textured = textured;
		this.pose = new Matrix3x2f(graphics.pose());
	}

	/** Untextured quads (position and colour, the vanilla GUI pipeline); the pose is the current one of {@code graphics}. */
	public static GuiQuads colored(GuiGraphicsExtractor graphics) {
		return new GuiQuads(graphics, RenderPipelines.GUI, TextureSetup.noTexture(), false);
	}

	/** Quads with position, UV and colour drawn by {@code pipeline} with {@code textureSetup}. */
	public static GuiQuads textured(GuiGraphicsExtractor graphics, RenderPipeline pipeline, TextureSetup textureSetup) {
		return new GuiQuads(graphics, pipeline, textureSetup, true);
	}

	/** Adds one vertex; every four vertices form a quad. */
	public GuiQuads vertex(float x, float y, int argb) {
		return vertex(x, y, 0, 0, argb);
	}

	/** Adds one textured vertex; every four vertices form a quad. */
	public GuiQuads vertex(float x, float y, float u, float v, int argb) {
		vertices.add(x);
		vertices.add(y);
		vertices.add(u);
		vertices.add(v);
		colors.add(argb);
		return this;
	}

	/** Submits the quads to the GUI render state of {@code graphics}; nothing is drawn without a complete quad. */
	public void submit(GuiGraphicsExtractor graphics) {
		if (colors.size() < 4) {
			return;
		}
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < vertices.size(); i += 4) {
			minX = Math.min(minX, vertices.getFloat(i));
			minY = Math.min(minY, vertices.getFloat(i + 1));
			maxX = Math.max(maxX, vertices.getFloat(i));
			maxY = Math.max(maxY, vertices.getFloat(i + 1));
		}
		int x0 = (int) Math.floor(minX);
		int y0 = (int) Math.floor(minY);
		bounds = new ScreenRectangle(x0, y0, (int) Math.ceil(maxX) - x0, (int) Math.ceil(maxY) - y0).transformMaxBounds(pose);
		((GuiGraphicsAccessor) graphics).sophisticatedbuilding$getGuiRenderState().addGuiElement(this);
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		int quadVertices = colors.size() - colors.size() % 4;
		for (int i = 0; i < quadVertices; i++) {
			int offset = i * 4;
			VertexConsumer vertex = consumer.addVertexWith2DPose(pose, vertices.getFloat(offset), vertices.getFloat(offset + 1));
			if (textured) {
				vertex.setUv(vertices.getFloat(offset + 2), vertices.getFloat(offset + 3));
			}
			vertex.setColor(colors.getInt(i));
		}
	}

	@Override
	public RenderPipeline pipeline() {
		return pipeline;
	}

	@Override
	public TextureSetup textureSetup() {
		return textureSetup;
	}

	@Nullable
	@Override
	public ScreenRectangle scissorArea() {
		return null;
	}

	@Nullable
	@Override
	public ScreenRectangle bounds() {
		return bounds;
	}
}
