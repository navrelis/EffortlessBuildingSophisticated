package sophisticated.building;

import com.mojang.blaze3d.systems.RenderSystem;
import sophisticated.building.utilities.Color;
import sophisticated.building.gui.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * GUI texture definitions for the mod.
 * Implements both our custom ScreenElement and Catnip's ScreenElement for compatibility.
 */
public enum AllGuiTextures implements ScreenElement, net.createmod.catnip.gui.element.ScreenElement {
    ARRAY_ENTRY("modifiers", 226, 64),
    MIRROR_ENTRY("modifiers", 0, 64, 226, 64),
    RADIAL_MIRROR_ENTRY("modifiers", 0, 128, 226, 64),
    ENABLE_BUTTON_BACKGROUND("modifiers", 234, 0, 9, 9),
    CHECKMARK("modifiers", 243, 0, 10, 9),
    ARROW_UP("modifiers", 234, 9, 9, 9),
    ARROW_DOWN("modifiers", 243, 9, 9, 9),
    TRASH("modifiers", 234, 18, 9, 9),
    ;
    public final ResourceLocation location;
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
        this.location = ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }
    
    @OnlyIn(Dist.CLIENT)
    public void bind() {
        RenderSystem.setShaderTexture(0, location);
    }
    
    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics ms, int x, int y) {
        ms.blit(location, x, y, 0, startX, startY, width, height, 256, 256);
    }
    
    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics ms, int x, int y, Color c) {
        // Simple colored render - just render normally since we don't have Catnip's UIRenderHelper
        bind();
        RenderSystem.setShaderColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        ms.blit(location, x, y, 0, startX, startY, width, height, 256, 256);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
