package sophisticated.building.gui.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import sophisticated.building.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TextComponent;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Checkbox widget used in mod GUI screens.
 */
@ParametersAreNonnullByDefault
public class GuiCheckBoxFixed extends Button {
	private final int boxWidth;
	private boolean isChecked;

	public GuiCheckBoxFixed(int xPos, int yPos, String displayString, boolean isChecked) {
		super(xPos, yPos, Minecraft.getInstance().font.width(displayString) + 2 + 11, 11, new TextComponent(displayString), b -> {
		});
		this.isChecked = isChecked;
		this.boxWidth = 11;
		this.height = 11;
		this.width = this.boxWidth + 2 + Minecraft.getInstance().font.width(displayString);
	}

	@Override
	public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partial) {
		GuiGraphics guiGraphics = new GuiGraphics(poseStack);
		// The disabled button texture, nine-sliced like vanilla buttons (the widget/button_disabled sprite of 1.20.2+)
		guiGraphics.blitNineSliced(WIDGETS_LOCATION, this.x, this.y, this.boxWidth, this.height, 20, 4, 200, 20, 0, 46);
		int color = 14737632;

		if (!this.active) {
			color = 10526880;
		}

		Font font = Minecraft.getInstance().font;

		if (this.isChecked)
			guiGraphics.drawCenteredString(font, "x", this.x + this.boxWidth / 2 + 1, this.y + 1, 14737632);

		guiGraphics.drawString(font, getMessage(), this.x + this.boxWidth + 2, this.y + 2, color, false);
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
