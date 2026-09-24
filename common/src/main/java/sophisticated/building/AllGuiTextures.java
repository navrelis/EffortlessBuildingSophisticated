package sophisticated.building;

import sophisticated.building.utilities.Color;
import sophisticated.building.gui.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * GUI texture definitions for the mod.
 */
public enum AllGuiTextures implements ScreenElement {
    ARRAY_ENTRY("modifiers", 226, 64),
    MIRROR_ENTRY("modifiers", 0, 64, 226, 64),
    RADIAL_MIRROR_ENTRY("modifiers", 0, 128, 226, 64),
    ENABLE_BUTTON_BACKGROUND("modifiers", 234, 0, 9, 9),
    CHECKMARK("modifiers", 243, 0, 10, 9),
    ARROW_UP("modifiers", 234, 9, 9, 9),
    ARROW_DOWN("modifiers", 243, 9, 9, 9),
    TRASH("modifiers", 234, 18, 9, 9),
    ;
    public final Identifier location;
    public int width, height;
    public int startX, startY;
    private AllGuiTextures(String location, int width, int height) {
        this(location, 0, 0, width, height);
    }
    
    private AllGuiTextures(int startX, int startY) {
        this("icons", startX * 16, startY * 16, 16, 16);
    }
    
    private AllGuiTextures(String location, int startX, int startY, int width, int height) {
        this(SophisticatedBuilding.MODID, location, startX, startY, width, height);
    }
    
    private AllGuiTextures(String namespace, String location, int startX, int startY, int width, int height) {
        this.location = Identifier.fromNamespaceAndPath(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }
    
    public void render(GuiGraphics ms, int x, int y) {
        ms.blit(RenderPipelines.GUI_TEXTURED, location, x, y, startX, startY, width, height, 256, 256);
    }
    
    public void render(GuiGraphics ms, int x, int y, Color c) {
        ms.blit(RenderPipelines.GUI_TEXTURED, location, x, y, startX, startY, width, height, 256, 256, c.toARGB());
    }
}

