package sophisticated.building.client.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

/**
 * The subset of Minecraft 1.20's {@code GuiGraphics} the mod's screens, widgets and HUD use, implemented with the
 * Minecraft 1.16.5 GUI API ({@link GuiComponent}'s static helpers on a {@link PoseStack}, the fixed-function
 * model-view matrix for items). Same method names, parameters and results as the 1.20 class, so the GUI code stays the
 * same as on the newer branches; vanilla render callbacks (which still take a {@link PoseStack} in 1.16.5) wrap their
 * stack with {@link #GuiGraphics(PoseStack)}.
 */
public final class GuiGraphics {

    private final Minecraft minecraft;
    private final PoseStack pose;

    public GuiGraphics(PoseStack pose) {
        this.minecraft = Minecraft.getInstance();
        this.pose = pose;
    }

    public PoseStack pose() {
        return pose;
    }

    /**
     * The fixed-function state of Minecraft 1.16.5 that stands in for the position-color shader of 1.17+ (as
     * {@link GuiComponent}'s gradient fill sets it): untextured, smooth-shaded vertex colors, no alpha test. Undo with
     * {@link #endPositionColor()} after the buffer was drawn.
     */
    public static void beginPositionColor() {
        RenderSystem.disableTexture();
        RenderSystem.disableAlphaTest();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
    }

    public static void endPositionColor() {
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }

    public int guiWidth() {
        return minecraft.getWindow().getGuiScaledWidth();
    }

    public int guiHeight() {
        return minecraft.getWindow().getGuiScaledHeight();
    }

    /** Draws what was batched into the shared buffer source (1.19.2 draws text and fills immediately). */
    public void flush() {
        RenderSystem.disableDepthTest();
        minecraft.renderBuffers().bufferSource().endBatch();
        RenderSystem.enableDepthTest();
    }

    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        GuiComponent.fill(pose, minX, minY, maxX, maxY, color);
    }

    public void fillGradient(int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
        Gradient.INSTANCE.fill(pose, minX, minY, maxX, maxY, colorFrom, colorTo);
    }

    public void renderOutline(int x, int y, int width, int height, int color) {
        fill(x, y, x + width, y + 1, color);
        fill(x, y + height - 1, x + width, y + height, color);
        fill(x, y + 1, x + 1, y + height - 1, color);
        fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /**
     * GUI coordinates to window pixels, as 1.19.2's {@code GuiComponent.enableScissor} (missing in 1.18.2) does. Straight
     * OpenGL calls: Minecraft 1.16.3 has no RenderSystem.enableScissor / disableScissor (1.16.4+) and no scissor state
     * in GlStateManager.
     */
    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        Window window = minecraft.getWindow();
        double scale = window.getGuiScale();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (minX * scale), (int) (window.getHeight() - maxY * scale),
                Math.max(0, (int) ((maxX - minX) * scale)), Math.max(0, (int) ((maxY - minY) * scale)));
    }

    public void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void blit(ResourceLocation atlas, int x, int y, int uOffset, int vOffset, int width, int height) {
        blit(atlas, x, y, 0, uOffset, vOffset, width, height, 256, 256);
    }

    public void blit(ResourceLocation atlas, int x, int y, int blitOffset, float uOffset, float vOffset, int width,
                     int height, int textureWidth, int textureHeight) {
        minecraft.getTextureManager().bind(atlas);
        GuiComponent.blit(pose, x, y, blitOffset, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    public void blit(ResourceLocation atlas, int x, int y, float uOffset, float vOffset, int width, int height,
                     int textureWidth, int textureHeight) {
        blit(atlas, x, y, 0, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    /**
     * Draws the {@code sourceWidth} x {@code sourceHeight} region at {@code (uOffset, vOffset)} stretched to
     * {@code width} x {@code height}: corners of {@code sliceWidth} x {@code sliceHeight} stay as they are, edges and
     * centre are tiled.
     */
    public void blitNineSliced(ResourceLocation atlas, int x, int y, int width, int height, int sliceWidth,
                               int sliceHeight, int sourceWidth, int sourceHeight, int uOffset, int vOffset) {
        int left = Math.min(sliceWidth, width / 2);
        int right = left;
        int top = Math.min(sliceHeight, height / 2);
        int bottom = top;
        int innerWidth = width - left - right;
        int innerHeight = height - top - bottom;
        int innerSourceWidth = sourceWidth - left - right;
        int innerSourceHeight = sourceHeight - top - bottom;
        blit(atlas, x, y, uOffset, vOffset, left, top);
        blit(atlas, x + width - right, y, uOffset + sourceWidth - right, vOffset, right, top);
        blit(atlas, x, y + height - bottom, uOffset, vOffset + sourceHeight - bottom, left, bottom);
        blit(atlas, x + width - right, y + height - bottom, uOffset + sourceWidth - right,
                vOffset + sourceHeight - bottom, right, bottom);
        blitRepeating(atlas, x + left, y, innerWidth, top, uOffset + left, vOffset, innerSourceWidth, top);
        blitRepeating(atlas, x + left, y + height - bottom, innerWidth, bottom, uOffset + left,
                vOffset + sourceHeight - bottom, innerSourceWidth, bottom);
        blitRepeating(atlas, x, y + top, left, innerHeight, uOffset, vOffset + top, left, innerSourceHeight);
        blitRepeating(atlas, x + width - right, y + top, right, innerHeight, uOffset + sourceWidth - right,
                vOffset + top, right, innerSourceHeight);
        blitRepeating(atlas, x + left, y + top, innerWidth, innerHeight, uOffset + left, vOffset + top,
                innerSourceWidth, innerSourceHeight);
    }

    private void blitRepeating(ResourceLocation atlas, int x, int y, int width, int height, int uOffset, int vOffset,
                               int sourceWidth, int sourceHeight) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }
        for (int dx = 0; dx < width; dx += sourceWidth) {
            for (int dy = 0; dy < height; dy += sourceHeight) {
                blit(atlas, x + dx, y + dy, uOffset, vOffset, Math.min(sourceWidth, width - dx),
                        Math.min(sourceHeight, height - dy));
            }
        }
    }

    public int drawString(Font font, String text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(Font font, String text, int x, int y, int color, boolean dropShadow) {
        if (text == null) {
            return 0;
        }
        return dropShadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public int drawString(Font font, Component text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(Font font, Component text, int x, int y, int color, boolean dropShadow) {
        return drawString(font, text.getVisualOrderText(), x, y, color, dropShadow);
    }

    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) {
        return drawString(font, text, x, y, color, true);
    }

    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color, boolean dropShadow) {
        return dropShadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public void drawCenteredString(Font font, String text, int x, int y, int color) {
        drawString(font, text, x - font.width(text) / 2, y, color);
    }

    public void drawCenteredString(Font font, Component text, int x, int y, int color) {
        FormattedCharSequence sequence = text.getVisualOrderText();
        drawString(font, sequence, x - font.width(sequence) / 2, y, color);
    }

    /** The item at the pose's position (1.16.5's item renderer draws with the fixed-function model-view matrix). */
    public void renderItem(ItemStack stack, int x, int y) {
        withPoseAsModelView(() -> minecraft.getItemRenderer().renderAndDecorateFakeItem(stack, x, y));
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        withPoseAsModelView(() -> minecraft.getItemRenderer().renderGuiItemDecorations(font, stack, x, y));
    }

    /** Needs an open screen, as every caller has (1.19.2 draws tooltips through {@link Screen}). */
    public void renderComponentTooltip(Font font, List<Component> lines, int x, int y) {
        Screen screen = minecraft.screen;
        if (screen != null) {
            screen.renderComponentTooltip(pose, lines, x, y);
        }
    }

    public void renderTooltip(Font font, Component text, int x, int y) {
        renderComponentTooltip(font, Collections.singletonList(text), x, y);
    }

    private void withPoseAsModelView(Runnable draw) {
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(pose.last().pose());
        try {
            draw.run();
        } finally {
            RenderSystem.popMatrix();
        }
    }

    /** Reaches {@link GuiComponent}'s protected gradient fill. */
    private static final class Gradient extends GuiComponent {
        private static final Gradient INSTANCE = new Gradient();

        private void fill(PoseStack pose, int minX, int minY, int maxX, int maxY, int colorFrom, int colorTo) {
            fillGradient(pose, minX, minY, maxX, maxY, colorFrom, colorTo);
        }
    }
}
