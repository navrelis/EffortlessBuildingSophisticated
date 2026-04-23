package sophisticated.building.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This class provides a checkbox style control.
 */
@ParametersAreNonnullByDefault
public class GuiCheckBoxFixed extends Button {
	// Sprite path is relative to textures/gui/sprites/ and no file extension
	private static final ResourceLocation CHECKBOX_SPRITE = ResourceLocation.withDefaultNamespace("widget/button_disabled");
	private final int boxWidth;
	private boolean isChecked;

	public GuiCheckBoxFixed(int xPos, int yPos, String displayString, boolean isChecked) {
		super(xPos, yPos, Minecraft.getInstance().font.width(displayString) + 2 + 11, 11, Component.literal(displayString), b -> {
		}, DEFAULT_NARRATION);
		this.isChecked = isChecked;
		this.boxWidth = 11;
		this.height = 11;
		this.width = this.boxWidth + 2 + Minecraft.getInstance().font.width(displayString);
	}

	@Override
	public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
		// Use blitSprite for modern nine-slice rendering
		guiGraphics.blitSprite(CHECKBOX_SPRITE, this.getX(), this.getY(), this.boxWidth, this.height);
		int color = 14737632;

		if (packedFGColor != 0) {
			color = packedFGColor;
		} else if (!this.active) {
			color = 10526880;
		}

		Font font = Minecraft.getInstance().font;

		if (this.isChecked)
			guiGraphics.drawCenteredString(font, "x", this.getX() + this.boxWidth / 2 + 1, this.getY() + 1, 14737632);

		guiGraphics.drawString(font, getMessage(), this.getX() + this.boxWidth + 2, this.getY() + 2, color, false);
	}

	@Override
	public void onPress() {
		this.isChecked = !this.isChecked;
	}

	public boolean isChecked() {
		return this.isChecked;
	}

	public void setIsChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}
}
