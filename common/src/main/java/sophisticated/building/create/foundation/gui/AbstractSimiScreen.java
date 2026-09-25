package sophisticated.building.create.foundation.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import sophisticated.building.create.catnip.animation.AnimationTickHolder;
import sophisticated.building.create.catnip.gui.TickableGuiEventListener;
import sophisticated.building.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import sophisticated.building.create.foundation.gui.widget.AbstractSimiWidget;
import sophisticated.building.create.foundation.utility.Components;
import sophisticated.building.gui.buildmodifier.ModifiersScreenList;
import sophisticated.building.platform.ClientServices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractSimiScreen extends Screen {

	protected int windowWidth, windowHeight;
	protected int windowXOffset, windowYOffset;
	protected int guiLeft, guiTop;

	// Minecraft 1.16.5's Screen only renders its buttons (AbstractWidgets); every renderable widget of this screen (buttons
	// and lists) is rendered from this list instead, in the order it was added, like the renderables of Minecraft 1.17+
	private final List<Widget> renderables = new ArrayList<>();

	protected AbstractSimiScreen(Component title) {
		super(title);
	}

	protected AbstractSimiScreen() {
		this(Components.immutableEmpty());
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowSize(int width, int height) {
		windowWidth = width;
		windowHeight = height;
	}

	/**
	 * This method must be called before {@code super.init()}!
	 */
	protected void setWindowOffset(int xOffset, int yOffset) {
		windowXOffset = xOffset;
		windowYOffset = yOffset;
	}

	@Override
	public void init(Minecraft minecraft, int width, int height) {
		// Screen.init(...) clears its widgets before init() adds them again (e.g. after a resize)
		renderables.clear();
		super.init(minecraft, width, height);
	}

	@Override
	protected void init() {
		guiLeft = (width - windowWidth) / 2;
		guiTop = (height - windowHeight) / 2;
		guiLeft += windowXOffset;
		guiTop += windowYOffset;
	}

	@Override
	public void tick() {
		for (GuiEventListener listener : children()) {
			if (listener instanceof TickableGuiEventListener) {
				TickableGuiEventListener tickable = (TickableGuiEventListener) listener;
				tickable.tick();
			}
		}
	}

	@Override
	public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
		if (getFocused() != null && !getFocused().isMouseOver(pMouseX, pMouseY))
			setFocused(null);
		return super.mouseClicked(pMouseX, pMouseY, pButton);
	}
	
	@Override
	public boolean isPauseScreen() {
		return false;
	}

	protected <W extends GuiEventListener & Widget> W addRenderableWidget(W widget) {
		renderables.add(widget);
		return addWidget(widget);
	}

	protected <W extends Widget> W addRenderableOnly(W widget) {
		renderables.add(widget);
		return widget;
	}

	protected void removeWidget(GuiEventListener widget) {
		renderables.remove(widget);
		children.remove(widget);
	}

	@SuppressWarnings("unchecked")
	protected <W extends GuiEventListener & Widget> void addRenderableWidgets(W... widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected <W extends GuiEventListener & Widget> void addRenderableWidgets(Collection<W> widgets) {
		for (W widget : widgets) {
			addRenderableWidget(widget);
		}
	}

	protected void removeWidgets(GuiEventListener... widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	protected void removeWidgets(Collection<? extends GuiEventListener> widgets) {
		for (GuiEventListener widget : widgets) {
			removeWidget(widget);
		}
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		GuiGraphics graphics = new GuiGraphics(poseStack);
		partialTicks = AnimationTickHolder.getPartialTicksUI();
		PoseStack ms = graphics.pose();
		
		ms.pushPose();

		prepareFrame();

		renderWindowBackground(graphics, mouseX, mouseY, partialTicks);
		for (Widget widget : renderables) {
			widget.render(graphics.pose(), mouseX, mouseY, partialTicks);
		}
		renderWindow(graphics, mouseX, mouseY, partialTicks);
		renderWindowForeground(graphics, mouseX, mouseY, partialTicks);

		endFrame();

		ms.popPose();
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
		if (keyPressed || getFocused() instanceof EditBox)
			return keyPressed;

		if (ClientServices.CLIENT.isActiveAndMatches(this.minecraft.options.keyInventory, keyCode, scanCode)) {
			this.onClose();
			return true;
		}

		return false;
	}

	protected void prepareFrame() {}

	protected void renderWindowBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(graphics.pose()); //Manually draw background
	}

	protected abstract void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);

	protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		for (GuiEventListener listener : children()) {
			if (listener instanceof Widget) {
				Widget widget = (Widget) listener;
				if (widget instanceof AbstractSimiWidget && ((AbstractSimiWidget) widget).isMouseOver(mouseX, mouseY)
					&& ((AbstractSimiWidget) widget).visible) {
					AbstractSimiWidget simiWidget = (AbstractSimiWidget) widget;
					List<Component> tooltip = simiWidget.getToolTip();
					if (tooltip.isEmpty()) {
						continue;
					}
					int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.getX();
					int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.getY();
					graphics.renderComponentTooltip(font, tooltip, ttx, tty);
				}

				if (widget instanceof ModifiersScreenList) {
					ModifiersScreenList list = (ModifiersScreenList) widget;
					list.renderWindowForeground(graphics, mouseX, mouseY, partialTicks);
				}
			}
		}
	}

	protected void endFrame() {}

	@Deprecated
	protected void debugWindowArea(GuiGraphics graphics) {
		graphics.fill(guiLeft + windowWidth, guiTop + windowHeight, guiLeft, guiTop, 0xD3D3D3D3);
	}

	@Override
	public GuiEventListener getFocused() {
		GuiEventListener focused = super.getFocused();
		if (focused instanceof AbstractWidget && !((AbstractWidget) focused).isFocused())
			focused = null;
		setFocused(focused);
		return focused;
	}

}

