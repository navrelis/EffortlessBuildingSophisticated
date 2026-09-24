package sophisticated.building.create.catnip.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;
import sophisticated.building.client.gui.GuiQuads;
import sophisticated.building.create.catnip.data.Couple;
import sophisticated.building.create.catnip.render.GuiPipelines;
import sophisticated.building.create.catnip.theme.Color;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.gui.UIRenderHelper}, MIT License, Copyright (c)
 * 2022 The Create Team, see LICENSE_Ponder.txt); only the gradient helpers are kept, plus the stencilled gradient that
 * replaces Catnip's GL stencil (see {@link GuiPipelines}). Everything is submitted to the GUI render state as
 * {@link GuiQuads}, layered like the vanilla GUI elements in submission order (the GUI has no depth since
 * Minecraft 1.21.6, so Catnip's z parameters are gone).
 */
public class UIRenderHelper {

	/**
	 * @see #angledGradient(GuiGraphicsExtractor, float, int, int, float, float, Color, Color)
	 */
	public static void angledGradient(GuiGraphicsExtractor graphics, float angle, int x, int y, float breadth, float length, Couple<Color> c) {
		angledGradient(graphics, angle, x, y, breadth, length, c.getFirst(), c.getSecond());
	}

	/**
	 * x and y specify the middle point of the starting edge
	 *
	 * @param angle      the angle of the gradient in degrees; 0° means from left to right
	 * @param startColor the color at the starting edge
	 * @param endColor   the color at the ending edge
	 * @param breadth    the total width of the gradient
	 */
	public static void angledGradient(GuiGraphicsExtractor graphics, float angle, int x, int y, float breadth, float length, Color startColor, Color endColor) {
		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();
		poseStack.translate(x, y);
		poseStack.rotate((float) Math.toRadians(angle - 90));

		float w = breadth / 2;
		GuiQuads.colored(graphics)
			.vertex(w, 0, startColor.getRGB())
			.vertex(-w, 0, startColor.getRGB())
			.vertex(-w, length, endColor.getRGB())
			.vertex(w, length, endColor.getRGB())
			.submit(graphics);

		poseStack.popMatrix();
	}

	/**
	 * Draws the gradient only where {@code stencil} is not transparent: the stencil region is drawn with the gradient's
	 * colour at each corner (the gradient is linear, so the interpolation across the quad matches it exactly).
	 */
	public static void stencilledGradient(GuiGraphicsExtractor graphics, TextureRegion stencil, Gradient gradient) {
		float x0 = stencil.x();
		float y0 = stencil.y();
		float x1 = x0 + stencil.width();
		float y1 = y0 + stencil.height();
		float u0 = stencil.u() / (float) stencil.textureWidth();
		float v0 = stencil.v() / (float) stencil.textureHeight();
		float u1 = (stencil.u() + stencil.width()) / (float) stencil.textureWidth();
		float v1 = (stencil.v() + stencil.height()) / (float) stencil.textureHeight();
		AbstractTexture stencilTexture = Minecraft.getInstance().getTextureManager().getTexture(stencil.texture());
		TextureSetup texture = TextureSetup.singleTexture(stencilTexture.getTextureView(), stencilTexture.getSampler());
		GuiQuads.textured(graphics, GuiPipelines.STENCIL_GRADIENT, texture)
			.vertex(x0, y0, u0, v0, gradient.colorAt(x0, y0))
			.vertex(x0, y1, u0, v1, gradient.colorAt(x0, y1))
			.vertex(x1, y1, u1, v1, gradient.colorAt(x1, y1))
			.vertex(x1, y0, u1, v0, gradient.colorAt(x1, y0))
			.submit(graphics);
	}

	/** A region of a texture drawn at (x, y) with its own size. */
	public record TextureRegion(Identifier texture, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight) {
	}

	/**
	 * A linear gradient as drawn by {@link #angledGradient(GuiGraphicsExtractor, float, int, int, float, float, Color, Color)}:
	 * it starts at (x, y) and runs {@code length} in the direction of {@code angle}.
	 */
	public record Gradient(float angle, float x, float y, float length, Color startColor, Color endColor) {

		/** The ARGB colour of the gradient at a point, clamped to the start and end colour beyond its edges. */
		public int colorAt(float px, float py) {
			double radians = Math.toRadians(angle);
			float along = (float) ((px - x) * Math.cos(radians) + (py - y) * Math.sin(radians));
			return Color.mixColors(startColor, endColor, Mth.clamp(along / length, 0, 1)).getRGB();
		}
	}
}
