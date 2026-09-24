package sophisticated.building.create.catnip.gui;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import sophisticated.building.create.catnip.data.Couple;
import sophisticated.building.create.catnip.render.GuiRenderTypes;
import sophisticated.building.create.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.gui.UIRenderHelper}, MIT License, Copyright (c)
 * 2022 The Create Team, see LICENSE_Ponder.txt); only the gradient helpers are kept, plus the stencilled gradient that
 * replaces Catnip's GL stencil (see {@link GuiRenderTypes}). Everything is drawn through the GUI buffer source of
 * {@link GuiGraphics}, so the draw order is the call order.
 */
public class UIRenderHelper {

	/**
	 * @see #angledGradient(GuiGraphics, float, int, int, int, float, float, Color, Color)
	 */
	public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, float breadth, float length, Couple<Color> c) {
		angledGradient(graphics, angle, x, y, 0, breadth, length, c);
	}

	/**
	 * @see #angledGradient(GuiGraphics, float, int, int, int, float, float, Color, Color)
	 */
	public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, int z, float breadth, float length, Couple<Color> c) {
		angledGradient(graphics, angle, x, y, z, breadth, length, c.getFirst(), c.getSecond());
	}

	/**
	 * @see #angledGradient(GuiGraphics, float, int, int, int, float, float, Color, Color)
	 */
	public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, float breadth, float length, Color color1, Color color2) {
		angledGradient(graphics, angle, x, y, 0, breadth, length, color1, color2);
	}

	/**
	 * x and y specify the middle point of the starting edge
	 *
	 * @param angle      the angle of the gradient in degrees; 0° means from left to right
	 * @param startColor the color at the starting edge
	 * @param endColor   the color at the ending edge
	 * @param breadth    the total width of the gradient
	 */
	public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, int z, float breadth, float length, Color startColor, Color endColor) {
		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(x, y, z);
		poseStack.mulPose(Axis.ZP.rotationDegrees(angle - 90));

		float w = breadth / 2;
		Matrix4f mat = poseStack.last().pose();
		graphics.drawSpecial(buffers -> drawGradientRect(buffers.getBuffer(RenderType.gui()), mat, 0, -w, 0f, w, length, startColor, endColor));

		poseStack.popPose();
	}

	public static void drawGradientRect(VertexConsumer buffer, Matrix4f mat, int zLevel, float left, float top, float right, float bottom, Color startColor, Color endColor) {
		buffer.addVertex(mat, right, top, zLevel).setColor(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), startColor.getAlpha());
		buffer.addVertex(mat, left, top, zLevel).setColor(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), startColor.getAlpha());
		buffer.addVertex(mat, left, bottom, zLevel).setColor(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), endColor.getAlpha());
		buffer.addVertex(mat, right, bottom, zLevel).setColor(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), endColor.getAlpha());
	}

	/**
	 * Draws the gradient only where {@code stencil} is not transparent: the stencil region is drawn with the gradient's
	 * colour at each corner (the gradient is linear, so the interpolation across the quad matches it exactly).
	 */
	public static void stencilledGradient(GuiGraphics graphics, TextureRegion stencil, Gradient gradient) {
		Matrix4f mat = graphics.pose().last().pose();
		float x0 = stencil.x();
		float y0 = stencil.y();
		float x1 = x0 + stencil.width();
		float y1 = y0 + stencil.height();
		float u0 = stencil.u() / (float) stencil.textureWidth();
		float v0 = stencil.v() / (float) stencil.textureHeight();
		float u1 = (stencil.u() + stencil.width()) / (float) stencil.textureWidth();
		float v1 = (stencil.v() + stencil.height()) / (float) stencil.textureHeight();
		graphics.drawSpecial(buffers -> {
			VertexConsumer buffer = buffers.getBuffer(GuiRenderTypes.stencilGradient(stencil.texture()));
			buffer.addVertex(mat, x0, y0, 0).setUv(u0, v0).setColor(gradient.colorAt(x0, y0));
			buffer.addVertex(mat, x0, y1, 0).setUv(u0, v1).setColor(gradient.colorAt(x0, y1));
			buffer.addVertex(mat, x1, y1, 0).setUv(u1, v1).setColor(gradient.colorAt(x1, y1));
			buffer.addVertex(mat, x1, y0, 0).setUv(u1, v0).setColor(gradient.colorAt(x1, y0));
		});
	}

	/** A region of a texture drawn at (x, y) with its own size. */
	public record TextureRegion(ResourceLocation texture, int x, int y, int width, int height, int u, int v, int textureWidth, int textureHeight) {
	}

	/**
	 * A linear gradient as drawn by {@link #angledGradient(GuiGraphics, float, int, int, int, float, float, Color, Color)}:
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
