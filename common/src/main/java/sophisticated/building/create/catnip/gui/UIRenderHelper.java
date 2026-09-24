package sophisticated.building.create.catnip.gui;

import com.mojang.math.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import sophisticated.building.create.catnip.data.Couple;
import sophisticated.building.create.catnip.theme.Color;
import sophisticated.building.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.gui.UIRenderHelper}, MIT License, Copyright (c)
 * 2022 The Create Team, see LICENSE_Ponder.txt); only the gradient helpers are kept.
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
		poseStack.mulPose(Vector3f.ZP.rotationDegrees(angle - 90));

		float w = breadth / 2;
		drawGradientRect(poseStack.last().pose(), 0, -w, 0f, w, length, startColor, endColor);

		poseStack.popPose();
	}

	public static void drawGradientRect(Matrix4f mat, int zLevel, float left, float top, float right, float bottom, Color startColor, Color endColor) {
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder buffer = tesselator.getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(mat, right, top, zLevel).color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), startColor.getAlpha()).endVertex();
		buffer.vertex(mat, left, top, zLevel).color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), startColor.getAlpha()).endVertex();
		buffer.vertex(mat, left, bottom, zLevel).color(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), endColor.getAlpha()).endVertex();
		buffer.vertex(mat, right, bottom, zLevel).color(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), endColor.getAlpha()).endVertex();
		buffer.end();
		BufferUploader.end(buffer);

		RenderSystem.disableBlend();
	}
}
