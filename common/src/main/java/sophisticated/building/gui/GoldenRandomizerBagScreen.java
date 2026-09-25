package sophisticated.building.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import sophisticated.building.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.item.GoldenRandomizerBagItem;
import sophisticated.building.utilities.InventoryHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
public class GoldenRandomizerBagScreen extends AbstractContainerScreen<GoldenRandomizerBagContainer> {
	private Inventory inventory;

	private static final ResourceLocation guiTextures = SophisticatedBuilding.asResource("textures/gui/container/goldenrandomizerbag.png");
	
	// Red overlay color (semi-transparent red)
	private static final int MISSING_ITEM_COLOR = 0x80FF0000;

	public GoldenRandomizerBagScreen(GoldenRandomizerBagContainer randomizerBagContainer, Inventory playerInventory, Component title) {
		super(randomizerBagContainer, playerInventory, title);
		this.inventory = playerInventory;
		imageHeight = 134;
	}

	@Override
	public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
		// The dimmed world behind the screen (drawn by super.render from 1.20.2 on)
		this.renderBackground(poseStack);
		super.render(poseStack, mouseX, mouseY, partialTicks);
		this.renderTooltip(poseStack, mouseX, mouseY);
		BagTitle.renderTooltip(new GuiGraphics(poseStack), this.font, this.title, leftPos, topPos, imageWidth, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
		GuiGraphics guiGraphics = new GuiGraphics(poseStack);
		BagTitle.draw(guiGraphics, this.font, this.title, imageWidth, 0x404040);
		guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
	}

	@Override
	protected void renderBg(PoseStack poseStack, float partialTicks, int mouseX, int mouseY) {
		GuiGraphics guiGraphics = new GuiGraphics(poseStack);
		int marginHorizontal = (width - imageWidth) / 2;
		int marginVertical = (height - imageHeight) / 2;
		guiGraphics.blit(guiTextures, marginHorizontal, marginVertical, 0, 0, imageWidth, imageHeight);
		
		// Render red overlay on slots where player doesn't have the item in inventory
		renderMissingItemOverlays(guiGraphics);
	}
	
	protected void renderMissingItemOverlays(GuiGraphics guiGraphics) {
		Set<Item> missingItems = getMissingItems();
		if (missingItems.isEmpty()) return;
		
		for (int i = 0; i < GoldenRandomizerBagItem.INV_SIZE; i++) {
			Slot slot = this.menu.getSlot(i);
			ItemStack stack = slot.getItem();
			
			if (!stack.isEmpty() && missingItems.contains(stack.getItem())) {
				int x = leftPos + slot.x;
				int y = topPos + slot.y;
				guiGraphics.fill(x, y, x + 16, y + 16, MISSING_ITEM_COLOR);
			}
		}
	}
	
	protected Set<Item> getMissingItems() {
		Set<Item> missing = new HashSet<>();
		if (inventory.player.isCreative()) return missing;
		
		for (int i = 0; i < GoldenRandomizerBagItem.INV_SIZE; i++) {
			Slot slot = this.menu.getSlot(i);
			ItemStack stack = slot.getItem();
			
			if (!stack.isEmpty()) {
				int available = InventoryHelper.findTotalItemsInInventory(inventory.player, stack.getItem());
				if (available <= 0) {
					missing.add(stack.getItem());
				}
			}
		}
		return missing;
	}
}

