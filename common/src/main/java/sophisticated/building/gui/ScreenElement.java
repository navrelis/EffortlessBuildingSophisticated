package sophisticated.building.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A simple interface for renderable GUI elements.
 * Standalone replacement for Catnip ScreenElement.
 */
public interface ScreenElement {
    
    /**
     * Render this element at the specified position.
     * @param graphics The graphics context
     * @param x The x position
     * @param y The y position
     */
    void render(GuiGraphics graphics, int x, int y);
}
