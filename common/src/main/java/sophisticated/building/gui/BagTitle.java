package sophisticated.building.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

/**
 * The title of a randomizer bag screen, kept inside the GUI texture ({@link TitleFit}): scaled down when it is too
 * wide, cut off with "..." below the minimum scale, and then shown in full as a tooltip while the mouse is over it.
 */
public final class BagTitle {

    /** Left edge of the title inside the texture (the vanilla container label position). */
    public static final int X = 8;
    public static final int Y = 6;
    /** Space kept free at the right edge of the texture. */
    public static final int RIGHT_MARGIN = 8;

    private BagTitle() {
    }

    /** Width available to the title in a screen whose texture is {@code imageWidth} wide. */
    public static int availableWidth(int imageWidth) {
        return imageWidth - X - RIGHT_MARGIN;
    }

    public static TitleFit fit(Font font, Component title, int imageWidth) {
        return TitleFit.fit(title.getString(), availableWidth(imageWidth), font::width);
    }

    /** Draws the title; call from {@code renderLabels} (coordinates relative to the texture's top left corner). */
    public static void draw(GuiGraphics guiGraphics, Font font, Component title, int imageWidth, int color) {
        TitleFit fit = fit(font, title, imageWidth);
        if (fit.scale() >= 1f) {
            guiGraphics.drawString(font, fit.text(), X, Y, color, false);
            return;
        }
        Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        // Keep the smaller text vertically centred on the original line
        pose.translate(X, Y + font.lineHeight * (1f - fit.scale()) / 2f);
        pose.scale(fit.scale(), fit.scale());
        guiGraphics.drawString(font, fit.text(), 0, 0, color, false);
        pose.popMatrix();
    }

    /** Whether the mouse (screen coordinates) is over the title of a screen whose texture starts at left/top. */
    public static boolean isHovered(int left, int top, int imageWidth, Font font, double mouseX, double mouseY) {
        return mouseX >= left + X && mouseX < left + X + availableWidth(imageWidth)
                && mouseY >= top + Y - 1 && mouseY < top + Y + font.lineHeight;
    }

    /** The full title as a tooltip while the mouse is over a cut-off title; call from {@code render}. */
    public static void renderTooltip(GuiGraphics guiGraphics, Font font, Component title, int left, int top, int imageWidth,
                                     int mouseX, int mouseY) {
        if (isHovered(left, top, imageWidth, font, mouseX, mouseY) && fit(font, title, imageWidth).truncated()) {
            guiGraphics.setTooltipForNextFrame(font, title, mouseX, mouseY);
        }
    }
}
