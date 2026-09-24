package sophisticated.building.create.catnip.gui.widget;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import sophisticated.building.create.catnip.data.Couple;
import sophisticated.building.create.catnip.gui.TickableGuiEventListener;
import sophisticated.building.create.catnip.theme.Color;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import sophisticated.building.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.gui.widget.AbstractSimiWidget}, MIT License, Copyright (c) 2022
 * The Create Team, see LICENSE_Ponder.txt).
 */
public abstract class AbstractSimiWidget extends AbstractWidget implements TickableGuiEventListener {

	public static final Color HEADER_RGB = new Color(0x5391e1, false);
	public static final Color HINT_RGB = new Color(0x96b7e0, false);

	public static final Couple<Color> COLOR_IDLE = Couple.create(
		new Color(0xdd_8ab6d6, true),
		new Color(0x90_8ab6d6, true)
	).map(Color::setImmutable);
	public static final Couple<Color> COLOR_HOVER = Couple.create(
		new Color(0xff_9abbd3, true),
		new Color(0xd0_9abbd3, true)
	).map(Color::setImmutable);
	public static final Couple<Color> COLOR_CLICK = Couple.create(
		new Color(0xff_ffffff, true),
		new Color(0xee_ffffff, true)
	).map(Color::setImmutable);
	public static final Couple<Color> COLOR_DISABLED = Couple.create(
		new Color(0x80_909090, true),
		new Color(0x60_909090, true)
	).map(Color::setImmutable);
	public static final Couple<Color> COLOR_SUCCESS = Couple.create(
		new Color(0xcc_88f788, true),
		new Color(0xcc_20cc20, true)
	).map(Color::setImmutable);
	public static final Couple<Color> COLOR_FAIL = Couple.create(
		new Color(0xcc_f78888, true),
		new Color(0xcc_cc2020, true)
	).map(Color::setImmutable);

	protected float z;
	protected boolean wasHovered = false;
	protected List<Component> toolTip = new LinkedList<>();
	protected BiConsumer<Integer, Integer> onClick = (_$, _$$) -> {
	};

	public int lockedTooltipX = -1;
	public int lockedTooltipY = -1;

	protected AbstractSimiWidget(int x, int y) {
		this(x, y, 16, 16);
	}

	protected AbstractSimiWidget(int x, int y, int width, int height) {
		this(x, y, width, height, CommonComponents.EMPTY);
	}

	protected AbstractSimiWidget(int x, int y, int width, int height, Component message) {
		super(x, y, width, height, message);
	}

	public <T extends AbstractSimiWidget> T withCallback(BiConsumer<Integer, Integer> cb) {
		this.onClick = cb;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends AbstractSimiWidget> T withCallback(Runnable cb) {
		return withCallback((_$, _$$) -> cb.run());
	}

	public <T extends AbstractSimiWidget> T atZLevel(float z) {
		this.z = z;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends AbstractSimiWidget> T setActive(boolean active) {
		this.active = active;
		return (T) this;
	}

	public List<Component> getToolTip() {
		return toolTip;
	}

	@Override
	public void tick() {
	}

	// Minecraft 1.19.2 widgets render through renderButton(PoseStack, ...): it hands over to renderWidget as on 1.20
	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		renderWidget(new GuiGraphics(poseStack), mouseX, mouseY, partialTicks);
	}

	// Catnip overrides the final AbstractWidget.render through an access widener; the vanilla render
	// only calls renderButton while visible, so the same steps run here instead.
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		isHovered = isMouseOver(mouseX, mouseY);
		beforeRender(graphics, mouseX, mouseY, partialTicks);
		doRender(graphics, mouseX, mouseY, partialTicks);
		afterRender(graphics, mouseX, mouseY, partialTicks);
		renderTooltip(graphics, mouseX, mouseY, partialTicks);
		wasHovered = isHoveredOrFocused();
	}

	protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		if (this.isHovered()) {
			List<Component> tooltip = this.getToolTip();
			if (tooltip.isEmpty())
				return;
			int ttx = this.lockedTooltipX == -1 ? mouseX : this.lockedTooltipX + this.getX();
			int tty = this.lockedTooltipY == -1 ? mouseY : this.lockedTooltipY + this.getY();

			Font font = Minecraft.getInstance().font;
			graphics.renderComponentTooltip(font, tooltip, ttx, tty);
		}
	}

	protected void beforeRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.pose().pushPose();
	}

	protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
	}

	protected void afterRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.pose().popPose();
	}

	public void runCallback(double mouseX, double mouseY) {
		onClick.accept((int) mouseX, (int) mouseY);
	}

	@Override
	protected boolean clicked(double mouseX, double mouseY) {
		return this.isMouseOver(mouseX, mouseY);
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		runCallback(mouseX, mouseY);
	}

	@Override
	public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
		defaultButtonNarrationText(pNarrationElementOutput);
	}

	public void setHeight(int value) {
		this.height = value;
	}

	// Minecraft 1.19.2 widgets have public x/y fields and no accessors: the accessors of 1.19.3+ for the shared code

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public boolean isHovered() {
		return isHovered;
	}
}
