package sophisticated.building.gui.buildmodifier;

import sophisticated.building.create.catnip.gui.widget.BoxWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.buildmodifier.Array;
import sophisticated.building.buildmodifier.BaseModifier;
import sophisticated.building.buildmodifier.Mirror;
import sophisticated.building.buildmodifier.RadialMirror;
import sophisticated.building.create.foundation.gui.AbstractSimiScreen;
import sophisticated.building.create.foundation.gui.AllIcons;
import sophisticated.building.create.foundation.utility.Components;
import sophisticated.building.platform.ClientServices;


public class ModifiersScreen extends AbstractSimiScreen {
	protected ModifiersScreenList list;
	protected BoxWidget addArrayButton;
	protected BoxWidget addMirrorButton;
	protected BoxWidget addRadialMirrorButton;
	protected BoxWidget closeButton;

	public ModifiersScreen() {
		super(Component.translatable("sophisticatedbuilding.screen.modifier_settings"));
	}

	@Override
	//Create buttons and labels and add them to buttonList/labelList
	protected void init() {
		super.init();

		int listWidth = Math.min(width - 80, 300);
		int yCenter = height / 2;
		int listL = this.width / 2 - listWidth / 2;
		int listR = this.width / 2 + listWidth / 2;

		list = new ModifiersScreenList(minecraft, listWidth, height - 80, 45, 68);
		list.setX(this.width / 2 - list.getWidth() / 2);

		addRenderableWidget(list);

		initScrollEntries();

		addArrayButton = new BoxWidget(listR - 90, 10, 20, 20)
			.withPadding(2, 2)
			.withCallback(() -> addModifier(new Array()));
		addArrayButton.showingElement(AllIcons.I_ADD.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(addArrayButton)));
		addArrayButton.getToolTip().add(Components.translatable("sophisticatedbuilding.gui.modifier.add_array"));
		
		addMirrorButton = new BoxWidget(listR - 60, 10, 20, 20)
			.withPadding(2, 2)
			.withCallback(() -> addModifier(new Mirror()));
		addMirrorButton.showingElement(AllIcons.I_ADD.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(addMirrorButton)));
		addMirrorButton.getToolTip().add(Components.translatable("sophisticatedbuilding.gui.modifier.add_mirror"));
		
		addRadialMirrorButton = new BoxWidget(listR - 30, 10, 20, 20)
			.withPadding(2, 2)
			.withCallback(() -> addModifier(new RadialMirror()));
		addRadialMirrorButton.showingElement(AllIcons.I_ADD.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(addRadialMirrorButton)));
		addRadialMirrorButton.getToolTip().add(Components.translatable("sophisticatedbuilding.gui.modifier.add_radial_mirror"));

		closeButton = new BoxWidget(listL - 30, yCenter - 10, 20, 20)
			.withPadding(2, 2)
			.withCallback(this::onClose);
		closeButton.showingElement(AllIcons.I_CONFIG_BACK.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(closeButton)));
		closeButton.getToolTip().add(Components.translatable("sophisticatedbuilding.gui.modifier.close"));

		addRenderableWidget(addArrayButton);
		addRenderableWidget(addMirrorButton);
		addRenderableWidget(addRadialMirrorButton);
		addRenderableWidget(closeButton);
	}

	private void initScrollEntries() {

		list.clearModifierEntries();
		var modifierSettingsList = SophisticatedBuildingClient.BUILD_MODIFIERS.getModifierSettingsList();
		for (BaseModifier modifier : modifierSettingsList) {
			var entry = createModifierPanel(modifier);
			if (entry != null) {
				list.addModifierEntry(entry);
			}
		}
	}

	private BaseModifierEntry createModifierPanel(BaseModifier modifier) {
		if (modifier instanceof Mirror) {
			return new MirrorEntry(this, modifier);
		} else if (modifier instanceof Array) {
			return new ArrayEntry(this, modifier);
		} else if (modifier instanceof RadialMirror) {
			return new RadialMirrorEntry(this, modifier);
		} else {
			return null;
		}
	}

	private void addModifier(BaseModifier modifier) {
		var entry = createModifierPanel(modifier);
		if (entry != null) {
			list.addModifierEntry(entry);
		}
		SophisticatedBuildingClient.BUILD_MODIFIERS.addModifierSettings(modifier);
	}
	
	public void removeModifier(BaseModifierEntry entry) {
		list.removeModifierEntry(entry);
		SophisticatedBuildingClient.BUILD_MODIFIERS.removeModifierSettings(entry.modifier);
	}
	
	public boolean canMoveUp(BaseModifierEntry modifierEntry) {
		int index = list.children().indexOf(modifierEntry);
		return index > 0;
	}
	
	public boolean canMoveDown(BaseModifierEntry modifierEntry) {
		int index = list.children().indexOf(modifierEntry);
		return index < list.children().size() - 1;
	}
	
	public void moveModifierUp(BaseModifierEntry modifierEntry) {
		int index = list.children().indexOf(modifierEntry);
		if (index == 0) return;
		
		list.swapModifierEntries(index, index - 1);
		SophisticatedBuildingClient.BUILD_MODIFIERS.moveUp(modifierEntry.modifier);
	}
	
	public void moveModifierDown(BaseModifierEntry modifierEntry) {
		int index = list.children().indexOf(modifierEntry);
		if (index == list.children().size() - 1) return;
		
		list.swapModifierEntries(index, index + 1);
		SophisticatedBuildingClient.BUILD_MODIFIERS.moveDown(modifierEntry.modifier);
	}
	
	@Override
	public void resize(int width, int height) {
		double scroll = list.scrollAmount();
		super.resize(width, height);
		list.setScrollAmount(scroll);
	}
	
	@Override
	protected void renderWindow(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
	
	}
	
	@Override
	public void onClose() {
		super.onClose();
		SophisticatedBuildingClient.BUILD_MODIFIERS.save();
	}
	
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (ClientServices.CLIENT.matchesKey(ClientEvents.keyBindings[1], event)) {
			onClose();
			return true;
		}

		return super.keyPressed(event);
	}
}

