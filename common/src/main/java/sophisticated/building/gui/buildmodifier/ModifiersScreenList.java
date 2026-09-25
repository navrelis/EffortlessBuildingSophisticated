package sophisticated.building.gui.buildmodifier;

import com.mojang.blaze3d.vertex.PoseStack;
import sophisticated.building.create.catnip.gui.TickableGuiEventListener;
import sophisticated.building.create.catnip.gui.UIRenderHelper;
import sophisticated.building.create.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import sophisticated.building.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import sophisticated.building.create.foundation.gui.widget.AbstractSimiWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//Based on Create's ConfigScreenList
public class ModifiersScreenList extends ObjectSelectionList<ModifiersScreenList.Entry> implements TickableGuiEventListener {

    public ModifiersScreenList(Minecraft mc, int width, int height, int y1, int itemHeight) {
        super(mc, width, height, y1, y1 + height, itemHeight);
        headerHeight = 3;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        GuiGraphics guiGraphics = new GuiGraphics(poseStack);
        Color c = new Color(0x60_000000);
        UIRenderHelper.angledGradient(guiGraphics, 90, x0 + width / 2, y0, width, 5, c, Color.TRANSPARENT_BLACK);
        UIRenderHelper.angledGradient(guiGraphics, -90, x0 + width / 2, y1, width, 5, c, Color.TRANSPARENT_BLACK);
        UIRenderHelper.angledGradient(guiGraphics, 0, x0, y0 + height / 2, height, 5, c, Color.TRANSPARENT_BLACK);
        UIRenderHelper.angledGradient(guiGraphics, 180, x1, y0 + height / 2, height, 5, c, Color.TRANSPARENT_BLACK);
        guiGraphics.fill(x0, y0, x1, y1, 0x80_000000);

        // Not the dirt background and dirt bands of the vanilla list: Minecraft 1.16.3 has no setRenderBackground /
        // setRenderTopAndBottom (1.16.4+), so the list body, scroll bar and decorations of
        // AbstractSelectionList.render are drawn here without them, over the translucent dark background above (like
        // the list background of the 1.21 builds)
        int rowLeft = getRowLeft();
        int rowTop = y0 + 4 - (int) getScrollAmount();
        renderList(poseStack, rowLeft, rowTop, mouseX, mouseY, partialTicks);
        renderScrollBar(guiGraphics);
        renderDecorations(poseStack, mouseX, mouseY);
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        int maxScroll = Math.max(0, getMaxPosition() - (y1 - y0 - 4));
        if (maxScroll <= 0) {
            return;
        }
        int left = getScrollbarPosition();
        int right = left + 6;
        int thumbHeight = (int) ((float) ((y1 - y0) * (y1 - y0)) / getMaxPosition());
        thumbHeight = Math.max(32, Math.min(thumbHeight, y1 - y0 - 8));
        int thumbTop = Math.max(y0, (int) getScrollAmount() * (y1 - y0 - thumbHeight) / maxScroll + y0);
        guiGraphics.fill(left, y0, right, y1, 0xFF_000000);
        guiGraphics.fill(left, thumbTop, right, thumbTop + thumbHeight, 0xFF_808080);
        guiGraphics.fill(left, thumbTop, right - 1, thumbTop + thumbHeight - 1, 0xFF_C0C0C0);
    }
    
    public void renderWindowForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderListForeground(guiGraphics, mouseX, mouseY, partialTicks);
    }
    
    protected void renderListForeground(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        int i = this.getRowLeft();
        int j = this.getRowWidth();
        int k = this.itemHeight - 4;
        int l = this.getItemCount();
        
        for(int i1 = 0; i1 < l; ++i1) {
            int j1 = this.getRowTop(i1);
            int k1 = j1 + itemHeight;
            if (k1 >= this.y0 && j1 <= this.y1) {
                renderItemForeground(guiGraphics, pMouseX, pMouseY, pPartialTick, i1, i, j1, j, k);
            }
        }
    }
    
    protected void renderItemForeground(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick, int pIndex, int pLeft, int pTop, int pWidth, int pHeight) {
        Entry e = this.getEntry(pIndex);
        e.renderForeground(guiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, Objects.equals(this.isMouseOver(pMouseX, pMouseY) ? this.getEntryAtPosition(pMouseX, pMouseY) : null, e), pPartialTick);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (children().stream().anyMatch(e -> e.mouseClicked(x, y, button)))
            return true;
        return super.mouseClicked(x, y, button);
    }
    
    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (children().stream().anyMatch(e -> e.keyPressed(pKeyCode, pScanCode, pModifiers)))
            return true;
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (children().stream().anyMatch(e -> e.charTyped(pCodePoint, pModifiers)))
            return true;
        return super.charTyped(pCodePoint, pModifiers);
    }
    
    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double delta) {
        if (children().stream().anyMatch(e -> e.mouseScrolled(pMouseX, pMouseY, delta)))
            return true;
        return super.mouseScrolled(pMouseX, pMouseY, delta);
    }
    
    public int getWidth() {
        return width;
    }

    @Override
    public int getRowWidth() {
        return width - 16;
    }

    @Override
    protected int getScrollbarPosition() {
        return x0 + this.width - 6;
    }

    @Override
    public void tick() {
        children().forEach(Entry::tick);
    }

    public static abstract class Entry extends ObjectSelectionList.Entry<Entry> implements TickableGuiEventListener {
        protected final ModifiersScreen screen;
        protected List<GuiEventListener> listeners;

        protected Entry(ModifiersScreen screen) {
            this.screen = screen;
            listeners = new ArrayList<>();
        }

        @Override
        public boolean mouseClicked(double x, double y, int button) {
            return getGuiListeners().stream().anyMatch(l -> l.mouseClicked(x, y, button));
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return getGuiListeners().stream().anyMatch(l -> l.keyPressed(keyCode, scanCode, modifiers));
        }

        @Override
        public boolean charTyped(char ch, int modifiers) {
            return getGuiListeners().stream().anyMatch(l -> l.charTyped(ch, modifiers));
        }
    
        @Override
        public boolean mouseScrolled(double x, double y, double delta) {
            return getGuiListeners().stream().anyMatch(l -> l.mouseScrolled(x, y, delta));
        }
    
        @Override
        public void render(PoseStack poseStack, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTicks) {
            GuiGraphics guiGraphics = new GuiGraphics(poseStack);
    
//            UIRenderHelper.streak(guiGraphics, 0, x - 10, y + height / 2, height - 6, width, 0xdd_000000);
//            UIRenderHelper.streak(guiGraphics, 180, x + (int) (width * 1.35f) + 10, y + height / 2, height - 6, width / 8 * 7, 0xdd_000000);
    
        }

        public void renderForeground(GuiGraphics guiGraphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
            for (GuiEventListener listener : listeners) {
                if (listener instanceof AbstractSimiWidget && (((AbstractSimiWidget) listener).isHovered() || ((AbstractSimiWidget) listener).isFocused())
                    && ((AbstractSimiWidget) listener).visible) {
                    AbstractSimiWidget simiWidget = (AbstractSimiWidget) listener;
                    List<Component> tooltip = simiWidget.getToolTip();
                    if (tooltip.isEmpty())
                        continue;
                    int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.getX();
                    int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.getY();
                    guiGraphics.renderComponentTooltip(getFont(), tooltip, ttx, tty);
                }
            }
        }

        @Override
        public void tick() {}

        public List<GuiEventListener> getGuiListeners() {
            return listeners;
        }
        
        public Font getFont() {
            return Minecraft.getInstance().font;
        }
    }
}
