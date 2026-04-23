package sophisticated.building.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.item.RandomizerBagItem;
import sophisticated.building.utilities.InventoryHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@Environment(EnvType.CLIENT)
@ParametersAreNonnullByDefault
public class RandomizerBagScreen extends AbstractContainerScreen<RandomizerBagContainer> {
	private Inventory inventory;

	private static final ResourceLocation guiTextures = SophisticatedBuilding.asResource("textures/gui/container/randomizerbag.png");
	
	// Red overlay color (semi-transparent red)
	private static final int MISSING_ITEM_COLOR = 0x80FF0000;

	public RandomizerBagScreen(RandomizerBagContainer randomizerBagContainer, Inventory playerInventory, Component title) {
		super(randomizerBagContainer, playerInventory, title);
		this.inventory = playerInventory;
		imageHeight = 134;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
		guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		int marginHorizontal = (width - imageWidth) / 2;
		int marginVertical = (height - imageHeight) / 2;
		guiGraphics.blit(guiTextures, marginHorizontal, marginVertical, 0, 0, imageWidth, imageHeight);
		
		// Render red overlay on slots where player doesn't have the item in inventory
		renderMissingItemOverlays(guiGraphics);
	}
	
	/**
	 * Renders a red semi-transparent overlay on bag slots where the template item
	 * is not available in the player's inventory.
	 */
	protected void renderMissingItemOverlays(GuiGraphics guiGraphics) {
		// Get set of missing items
		Set<Item> missingItems = getMissingItems();
		if (missingItems.isEmpty()) return;
		
		// Check each bag slot (first N slots are bag slots)
		for (int i = 0; i < RandomizerBagItem.INV_SIZE; i++) {
			Slot slot = this.menu.getSlot(i);
			if (slot == null) {
				continue;
			}
			ItemStack stack = slot.getItem();
			
			if (!stack.isEmpty() && missingItems.contains(stack.getItem())) {
				// This slot has an item that's missing from inventory - render red overlay
				int x = leftPos + slot.x;
				int y = topPos + slot.y;
				guiGraphics.fill(x, y, x + 16, y + 16, MISSING_ITEM_COLOR);
			}
		}
	}
	
	/**
	 * Get set of items that are in the bag but missing from player inventory.
	 */
	protected Set<Item> getMissingItems() {
		Set<Item> missing = new HashSet<>();
		
		// Skip for creative mode players
		if (inventory.player == null) return missing;
		if (inventory.player.isCreative()) return missing;
		
		// Check each bag slot
		for (int i = 0; i < RandomizerBagItem.INV_SIZE; i++) {
			Slot slot = this.menu.getSlot(i);
			if (slot == null) {
				continue;
			}
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

