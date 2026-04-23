package sophisticated.building.gui.elements;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import sophisticated.building.create.foundation.gui.widget.Label;
import sophisticated.building.create.foundation.gui.widget.ScrollInput;
import sophisticated.building.create.foundation.utility.Components;
import sophisticated.building.create.foundation.utility.Lang;
import sophisticated.building.create.AllKeys;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

/**
 * Extended ScrollInput with keyboard support for Minecraft 1.21.1+
 * where scroll wheel input may not work in some GUI contexts.
 * 
 * Supports:
 * - Scroll wheel (when working)
 * - Arrow keys (Up/Right = increase, Down/Left = decrease)
 * - +/- keys for increment/decrement
 * - PageUp/PageDown for larger steps
 * - Click to focus, then use keyboard
 */
public class LabeledScrollInput extends ScrollInput {
    protected Label label;
    protected final Component controlScrollsSlowerText = Lang.translateDirect("gui.scrollInput.controlScrollsSlower");
    protected final Component keyboardControlsText = Component.literal("Arrow keys / +/- to adjust").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
    protected boolean controlScrollsSlower;
    protected boolean focused = false;
    
    public LabeledScrollInput(int xIn, int yIn, int widthIn, int heightIn) {
        super(xIn, yIn, widthIn, heightIn);
        min = Integer.MIN_VALUE;
        max = Integer.MAX_VALUE;
        
        label = new Label(0, 0, Components.immutableEmpty()).withShadow();
        writingTo(label);
    }
    
    public LabeledScrollInput showControlScrollsSlowerTooltip() {
        controlScrollsSlower = true;
        return this;
    }

    @Override
    public void doRender(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.doRender(guiGraphics, mouseX, mouseY, partialTicks);

        label.setX(getX() + width / 2 - Minecraft.getInstance().font.width(label.text) / 2);
        label.setY(getY() + height / 2 - Minecraft.getInstance().font.lineHeight / 2);
        label.render(guiGraphics, mouseX, mouseY, partialTicks);
        
        // Draw focus indicator when focused
        if (focused && visible) {
            int borderColor = 0xFFFFFF55; // Yellow tint for focus
            guiGraphics.renderOutline(getX() - 1, getY() - 1, width + 2, height + 2, borderColor);
        }
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
        // Toggle focus on click
        focused = true;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.visible && this.isHovered) {
            focused = true;
            return super.mouseClicked(mouseX, mouseY, button);
        } else {
            // Clicked elsewhere, unfocus
            focused = false;
        }
        return false;
    }
    
    /**
     * Handle keyboard input for value adjustment.
     * Works when widget is hovered OR focused.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.visible) return false;
        if (!this.isHovered && !focused) return false;
        
        int priorState = state;
        boolean shifted = AllKeys.shiftDown();
        int stepSize = shifted ? shiftStep : 1;
        int largeStep = shiftStep * 5; // For PageUp/PageDown
        
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP:
            case GLFW.GLFW_KEY_RIGHT:
            case GLFW.GLFW_KEY_KP_ADD:
            case GLFW.GLFW_KEY_EQUAL: // + key (shift+= on US keyboards)
                state += stepSize;
                break;
                
            case GLFW.GLFW_KEY_DOWN:
            case GLFW.GLFW_KEY_LEFT:
            case GLFW.GLFW_KEY_KP_SUBTRACT:
            case GLFW.GLFW_KEY_MINUS:
                state -= stepSize;
                break;
                
            case GLFW.GLFW_KEY_PAGE_UP:
                state += largeStep;
                break;
                
            case GLFW.GLFW_KEY_PAGE_DOWN:
                state -= largeStep;
                break;
                
            case GLFW.GLFW_KEY_HOME:
                state = min;
                break;
                
            case GLFW.GLFW_KEY_END:
                state = max - 1;
                break;
                
            default:
                return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        clampState();
        
        if (priorState != state) {
            onChanged();
        }
        
        return true;
    }
    
    @Override
    protected void updateTooltip() {
        super.updateTooltip();
        // Always show keyboard controls hint
        toolTip.add(keyboardControlsText);
        if (title == null || !controlScrollsSlower)
            return;
        toolTip.add(controlScrollsSlowerText.plainCopy()
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
    }
}
