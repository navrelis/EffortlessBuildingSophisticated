package sophisticated.building.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.item.OmegaRandomizerBagItem;
import sophisticated.building.network.message.OmegaBagWeightPacket;
import sophisticated.building.utilities.InventoryHelper;
import sophisticated.building.platform.Services;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
public class OmegaRandomizerBagScreen extends AbstractContainerScreen<OmegaRandomizerBagContainer> {
	private Inventory inventory;

	private static final ResourceLocation guiTextures = SophisticatedBuilding.asResource("textures/gui/container/omegarandomizerbag.png");
	
	// Red overlay color (semi-transparent red)
	private static final int MISSING_ITEM_COLOR = 0x80FF0000;
	
	private Button resetWeightsButton;

	public OmegaRandomizerBagScreen(OmegaRandomizerBagContainer randomizerBagContainer, Inventory playerInventory, Component title) {
		super(randomizerBagContainer, playerInventory, title);
		this.inventory = playerInventory;
		imageHeight = 221; // Taller to fit 6 rows + player inventory
		imageWidth = 176;
	}

	@Override
	protected void init() {
		super.init();
		
		// Add reset weights button to the right of the GUI
		int buttonX = leftPos + imageWidth + 4;
		int buttonY = topPos + 4;
		resetWeightsButton = Button.builder(Component.translatable("sophisticatedbuilding.gui.omega_bag.reset"), this::onResetWeightsPressed)
				.bounds(buttonX, buttonY, 40, 16)
				.build();
		this.addRenderableWidget(resetWeightsButton);
	}
	
	private void onResetWeightsPressed(Button button) {
		if (Minecraft.getInstance().player == null) {
			return;
		}
		ItemStack heldBag = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
		if (heldBag.getItem() instanceof OmegaRandomizerBagItem omegaBag) {
			// Reset all weights to 1
			for (int i = 0; i < OmegaRandomizerBagItem.INV_SIZE; i++) {
				Slot slot = this.menu.getSlot(i);
				if (slot != null && !slot.getItem().isEmpty()) {
					int currentWeight = omegaBag.getSlotWeight(heldBag, i);
					if (currentWeight != 1) {
						Services.NETWORK.sendToServer(new OmegaBagWeightPacket(i, 1));
						omegaBag.setSlotWeight(heldBag, i, 1);
					}
				}
			}
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		BagTitle.renderTooltip(guiGraphics, this.font, this.title, leftPos, topPos, imageWidth, mouseX, mouseY);
		
		// Render weight badges and tooltips
		renderWeightBadges(guiGraphics, mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (Minecraft.getInstance().player == null) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		// Check if mouse is over a template slot
		for (int i = 0; i < OmegaRandomizerBagItem.INV_SIZE; i++) {
			Slot slot = this.menu.getSlot(i);
			if (slot == null) {
				continue;
			}
			int slotX = leftPos + slot.x;
			int slotY = topPos + slot.y;
			
			if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
				ItemStack heldBag = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
				if (heldBag.getItem() instanceof OmegaRandomizerBagItem omegaBag) {
					int currentWeight = omegaBag.getSlotWeight(heldBag, i);
					int newWeight = currentWeight;
					
					// Weight steps: 1, 2, 3, 4, 5, 7, 10, 15, 20, 30, 40, 50, 65, 80, 90
					int[] weightSteps = {1, 2, 3, 4, 5, 7, 10, 15, 20, 30, 40, 50, 65, 80, 90};
					int currentIndex = 0;
					for (int j = 0; j < weightSteps.length; j++) {
						if (weightSteps[j] == currentWeight) { currentIndex = j; break; }
						if (weightSteps[j] > currentWeight) { currentIndex = Math.max(0, j - 1); break; }
					}
					
					if (scrollY > 0 && currentIndex < weightSteps.length - 1) { // Scroll up = increase
						newWeight = weightSteps[currentIndex + 1];
					} else if (scrollY < 0 && currentIndex > 0) { // Scroll down = decrease
						newWeight = weightSteps[currentIndex - 1];
					}
					
					if (newWeight != currentWeight) {
						// Send packet to server
						Services.NETWORK.sendToServer(new OmegaBagWeightPacket(i, newWeight));
						// Update locally for instant feedback
						omegaBag.setSlotWeight(heldBag, i, newWeight);
					}
				}
				return true;
			}
		}
		
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		BagTitle.draw(guiGraphics, this.font, this.title, imageWidth, 0xFF404040);
		guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, imageHeight - 94, 0xFF404040, false);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		int marginHorizontal = (width - imageWidth) / 2;
		int marginVertical = (height - imageHeight) / 2;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, guiTextures, marginHorizontal, marginVertical, 0, 0, imageWidth, imageHeight, 256, 256);
		
		// Render red overlay on slots where player doesn't have the item in inventory
		renderMissingItemOverlays(guiGraphics);
	}
	
	protected void renderMissingItemOverlays(GuiGraphics guiGraphics) {
		Set<Item> missingItems = getMissingItems();
		if (missingItems.isEmpty()) return;
		
		for (int i = 0; i < OmegaRandomizerBagItem.INV_SIZE; i++) {
			Slot slot = this.menu.getSlot(i);
			if (slot == null) {
				continue;
			}
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
		if (inventory.player == null) return missing;
		if (inventory.player.isCreative()) return missing;
		
		for (int i = 0; i < OmegaRandomizerBagItem.INV_SIZE; i++) {
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
	
	protected void renderWeightBadges(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if (Minecraft.getInstance().player == null) {
			return;
		}
		ItemStack heldBag = Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND);
		if (!(heldBag.getItem() instanceof OmegaRandomizerBagItem omegaBag)) return;
		
		Font font = Minecraft.getInstance().font;
		
		// Calculate total weight for percentage tooltip
		int totalWeight = 0;
		int[] weights = new int[OmegaRandomizerBagItem.INV_SIZE];
		for (int i = 0; i < OmegaRandomizerBagItem.INV_SIZE; i++) {
			Slot slot = this.menu.getSlot(i);
			if (slot == null) {
				continue;
			}
			if (!slot.getItem().isEmpty()) {
				weights[i] = omegaBag.getSlotWeight(heldBag, i);
				totalWeight += weights[i];
			}
		}
		
		// Render weight badges
		List<Component> hoveredTooltip = null;
		for (int i = 0; i < OmegaRandomizerBagItem.INV_SIZE; i++) {
			Slot slot = this.menu.getSlot(i);
			if (slot == null || slot.getItem().isEmpty()) continue;
			
			int weight = weights[i];
			int slotX = leftPos + slot.x;
			int slotY = topPos + slot.y;
			
			// Render weight badge in top-right corner of slot
			String weightText = String.valueOf(weight);
			int textWidth = font.width(weightText);
			int badgeX = slotX + 16 - textWidth - 2;
			int badgeY = slotY + 1;
			int badgeHeight = font.lineHeight;
			
			// Color gradient from green (low weight) to dark red (high weight)
			int color;
			if (weight <= 1) color = 0xFF00FF00; // Bright green
			else if (weight <= 4) color = 0xFF55FF55; // Light green
			else if (weight <= 7) color = 0xFF88FF00; // Yellow-green
			else if (weight <= 15) color = 0xFFFFFF00; // Yellow
			else if (weight <= 30) color = 0xFFFFAA00; // Orange
			else if (weight <= 50) color = 0xFFFF5500; // Dark orange
			else if (weight <= 70) color = 0xFFFF2200; // Red-orange
			else color = 0xFFCC0000; // Dark red for 71+
			
			// Draw a subtle background behind the weight for readability
			guiGraphics.fill(badgeX - 2, badgeY - 1, badgeX + textWidth + 2, badgeY + badgeHeight, 0xAA000000);

			guiGraphics.drawString(font, weightText, badgeX, badgeY, color, true);
			
			// Check if mouse is hovering over this slot for tooltip
			if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16 && totalWeight > 0) {
				float percentage = (weight * 100.0f) / totalWeight;
				List<Component> tooltip = new ArrayList<>();
				tooltip.add(Component.translatable("sophisticatedbuilding.gui.omega_bag.weight", weight).withStyle(ChatFormatting.GOLD));
				tooltip.add(Component.translatable("sophisticatedbuilding.gui.omega_bag.chance", String.format("%.1f", percentage)).withStyle(ChatFormatting.YELLOW));
				tooltip.add(Component.translatable("sophisticatedbuilding.gui.omega_bag.scroll").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
				
				hoveredTooltip = tooltip;
			}
		}

		if (hoveredTooltip != null) {
			// Offset tooltip to not overlap with item name tooltip (render below and to the right). GuiGraphics keeps only
			// one deferred tooltip per frame (the item tooltip), so this one is drawn now, in a stratum above the badges.
			guiGraphics.nextStratum();
			guiGraphics.renderTooltip(font, hoveredTooltip.stream().map(line -> ClientTooltipComponent.create(line.getVisualOrderText())).toList(),
				mouseX + 12, mouseY + 24, DefaultTooltipPositioner.INSTANCE, null);
		}
	}
}

