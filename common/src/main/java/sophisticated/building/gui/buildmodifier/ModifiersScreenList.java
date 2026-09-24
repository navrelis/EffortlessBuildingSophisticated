package sophisticated.building.gui.buildmodifier;

import sophisticated.building.create.catnip.gui.TickableGuiEventListener;
import sophisticated.building.create.catnip.gui.UIRenderHelper;
import sophisticated.building.create.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import sophisticated.building.create.foundation.gui.widget.AbstractSimiWidget;
import sophisticated.building.create.foundation.utility.Components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

//Based on Create's ConfigScreenList
public class ModifiersScreenList extends ObjectSelectionList<ModifiersScreenList.Entry> implements TickableGuiEventListener {

    /** Gap above the first entry (the removed AbstractSelectionList#headerHeight of Minecraft 1.21.8 and older). */
    public static final int HEADER_GAP = 3;

    public ModifiersScreenList(Minecraft mc, int width, int height, int y1, int itemHeight) {
        super(mc, width, height, y1, itemHeight);
//        setRenderBackground(false);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Color c = new Color(0x60_000000);
        UIRenderHelper.angledGradient(guiGraphics, 90, getX() + width / 2, getY(), width, 5, c, Color.TRANSPARENT_BLACK);
        UIRenderHelper.angledGradient(guiGraphics, -90, getX() + width / 2, getY() + getHeight(), width, 5, c, Color.TRANSPARENT_BLACK);
        UIRenderHelper.angledGradient(guiGraphics, 0, getX(), getY() + height / 2, height, 5, c, Color.TRANSPARENT_BLACK);
        UIRenderHelper.angledGradient(guiGraphics, 180, getX() + getWidth(), getY() + height / 2, height, 5, c, Color.TRANSPARENT_BLACK);

        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, partialTicks);
    }
    
    public void renderWindowForeground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderListForeground(guiGraphics, mouseX, mouseY, partialTicks);
    }
    
    protected void renderListForeground(GuiGraphicsExtractor guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        for (Entry e : children()) {
            if (e.getY() + e.getHeight() >= this.getY() && e.getY() <= (this.getY() + this.getHeight())) {
                e.renderForeground(guiGraphics, pMouseX, pMouseY, Objects.equals(this.getHovered(), e), pPartialTick);
            }
        }
    }

    /** Minecraft 1.21.9+: the entries are positioned by the list, so they are only added, removed and moved through it. */
    public void addModifierEntry(Entry entry) {
        addEntry(entry);
    }

    public void removeModifierEntry(Entry entry) {
        removeEntry(entry);
    }

    /** Swaps two entries and keeps the scroll position (vanilla swap would scroll to the moved entry). */
    public void swapModifierEntries(int index, int otherIndex) {
        double scroll = scrollAmount();
        List<Entry> entries = new ArrayList<>(children());
        Collections.swap(entries, index, otherIndex);
        replaceEntries(entries);
        setScrollAmount(scroll);
    }

    public void clearModifierEntries() {
        clearEntries();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (children().stream().anyMatch(e -> e.mouseClicked(event, doubleClick)))
            return true;
        return super.mouseClicked(event, doubleClick);
    }
    
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (children().stream().anyMatch(e -> e.keyPressed(event)))
            return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (children().stream().anyMatch(e -> e.charTyped(event)))
            return true;
        return super.charTyped(event);
    }
    
    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double scrollX, double scrollY) {
        if (children().stream().anyMatch(e -> e.mouseScrolled(pMouseX, pMouseY, scrollX, scrollY)))
            return true;
        return super.mouseScrolled(pMouseX, pMouseY, scrollX, scrollY);
    }
    
    @Override
    public int getRowWidth() {
        return width - 16;
    }

    @Override
    protected int scrollBarX() {
        return getX() + this.width - 6;
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
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return getGuiListeners().stream().anyMatch(l -> l.mouseClicked(event, doubleClick));
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            return getGuiListeners().stream().anyMatch(l -> l.keyPressed(event));
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            return getGuiListeners().stream().anyMatch(l -> l.charTyped(event));
        }
    
        @Override
        public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
            return getGuiListeners().stream().anyMatch(l -> l.mouseScrolled(x, y, scrollX, scrollY));
        }
    
        @Override
        public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTicks) {
    
//            UIRenderHelper.streak(guiGraphics, 0, x - 10, y + height / 2, height - 6, width, 0xdd_000000);
//            UIRenderHelper.streak(guiGraphics, 180, x + (int) (width * 1.35f) + 10, y + height / 2, height - 6, width / 8 * 7, 0xdd_000000);
    
        }

        public void renderForeground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
            for (var listener : listeners) {
                if (listener instanceof AbstractSimiWidget simiWidget && simiWidget.isHoveredOrFocused()
                    && simiWidget.visible) {
                    List<Component> tooltip = simiWidget.getToolTip();
                    if (tooltip.isEmpty())
                        continue;
                    int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.getX();
                    int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.getY();
                    guiGraphics.setComponentTooltipForNextFrame(getFont(), tooltip, ttx, tty);
                }
            }
        }

        @Override
        public void tick() {}

        public List<GuiEventListener> getGuiListeners() {
            return listeners;
        }

        @Override
        public Component getNarration() {
            return Components.immutableEmpty();
        }
        
        public Font getFont() {
            return Minecraft.getInstance().font;
        }
    }
}
