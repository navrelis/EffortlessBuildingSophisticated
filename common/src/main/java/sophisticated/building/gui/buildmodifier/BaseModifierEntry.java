package sophisticated.building.gui.buildmodifier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import sophisticated.building.AllGuiTextures;
import sophisticated.building.buildmodifier.BaseModifier;
import sophisticated.building.create.foundation.gui.widget.Label;
import sophisticated.building.create.foundation.utility.Components;
import sophisticated.building.gui.elements.MiniButton;

public abstract class BaseModifierEntry<T extends BaseModifier> extends ModifiersScreenList.Entry {

    public T modifier;
    protected AllGuiTextures background;
    protected MiniButton enableButton;
    protected Label nameLabel;
    protected MiniButton moveUpButton;
    protected MiniButton moveDownButton;
    protected MiniButton removeButton;
    protected Label reachLabel;
    
    protected static final int BACKGROUND_WIDTH = 226;
    protected static final int BACKGROUND_HEIGHT = 60;
    protected int left = 0;
    protected int right = 0;
    protected int top = 0;
    protected int bottom = 0;
    

    public BaseModifierEntry(ModifiersScreen screen, T modifier, Component name, AllGuiTextures background) {
        super(screen);

        this.modifier = modifier;
        this.background = background;

        enableButton = new MiniButton(0, 0, 100, 9)
            .showing(AllGuiTextures.ENABLE_BUTTON_BACKGROUND)
            .withCallback(() -> {
                modifier.enabled = !modifier.enabled;
                onValueChanged();
            });
        listeners.add(enableButton);
        
        nameLabel = new Label(65, 8, name);
        nameLabel.text = name;
        
        moveUpButton = new MiniButton(0, 0, 9, 9)
            .showing(AllGuiTextures.ARROW_UP)
            .withCallback(() -> {
                screen.moveModifierUp(this);
                onValueChanged();
            });
        moveUpButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.move_up"));
        listeners.add(moveUpButton);
        
        moveDownButton = new MiniButton(0, 0, 9, 9)
            .showing(AllGuiTextures.ARROW_DOWN)
            .withCallback(() -> {
                screen.moveModifierDown(this);
                onValueChanged();
            });
        moveDownButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.move_down"));
        listeners.add(moveDownButton);
        
        removeButton = new MiniButton(0, 0, 9, 9)
            .showing(AllGuiTextures.TRASH)
            .withCallback(() -> {
                screen.removeModifier(this);
            });
        removeButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.remove"));
        listeners.add(removeButton);
        
        reachLabel = new Label(0, 0, Components.immutableEmpty()).withShadow();
        listeners.add(reachLabel);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTicks) {

        left = getX() + getWidth() / 2 - BACKGROUND_WIDTH / 2;
        right = getX() + getWidth() / 2 + BACKGROUND_WIDTH / 2;
        top = getContentY() + ModifiersScreenList.HEADER_GAP;
        bottom = top + BACKGROUND_HEIGHT;
        
        background.render(guiGraphics, left, top);
        
        enableButton.setX(left + 4);
        enableButton.setY(top + 3);
        enableButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        if (modifier.enabled)
            AllGuiTextures.CHECKMARK.render(guiGraphics, left + 5, top + 3);
        
        nameLabel.setX(left + 18);
        nameLabel.setY(top + 4);
        nameLabel.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
    
        moveUpButton.visible = screen.canMoveUp(this);
        moveDownButton.visible = screen.canMoveDown(this);
        
        moveUpButton.setX(right - 31);
        moveUpButton.setY(top + 3);
        moveUpButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        
        moveDownButton.setX(right - 22);
        moveDownButton.setY(top + 3);
        moveDownButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        
        removeButton.setX(right - 13);
        removeButton.setY(top + 3);
        removeButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
    }

    public void onValueChanged() {
        if (modifier.enabled)
            enableButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.disable"));
        else
            enableButton.setToolTip(Components.translatable("sophisticatedbuilding.gui.modifier.enable"));
    }
}
