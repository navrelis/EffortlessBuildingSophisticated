package sophisticated.building.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.item.AbstractRandomizerBagItem;
import sophisticated.building.systems.BuilderChain;
import sophisticated.building.utilities.InventoryHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main render class for Sophisticated Building.
 */
public class RenderHandler {

	private static final ByteBufferBuilder LEVEL_BUFFER = new ByteBufferBuilder(1536);
	private static final ByteBufferBuilder GUI_BUFFER = new ByteBufferBuilder(1536);
	private static final ByteBufferBuilder TEXT_BUFFER = new ByteBufferBuilder(1536);

	public static void onRenderWorld(WorldRenderContext context) {
		
		Minecraft mc = Minecraft.getInstance();
		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

		PoseStack ms = context.matrixStack();
		if (ms == null) {
			return;
		}
		MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(LEVEL_BUFFER);

		ms.pushPose();
		ms.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

		//Mirror and radial mirror lines and areas
		ModifierRenderer.render(ms, buffer);
		
		try {
			renderGhostBlocksWithCatnip(ms);
		} catch (NoClassDefFoundError ignored) {
		}

		ms.popPose();

//		renderSubText(ms);
	}

	/**
	 * Render ghost blocks using Catnip's SuperRenderTypeBuffer.
	 */
	private static void renderGhostBlocksWithCatnip(PoseStack ms) {
		net.createmod.catnip.render.SuperRenderTypeBuffer ghostBuffer = net.createmod.catnip.render.DefaultSuperRenderTypeBuffer.getInstance();
		sophisticated.building.create.CreateClient.GHOST_BLOCKS.renderAll(ms, ghostBuffer);
		ghostBuffer.draw();
	}

	public static void onRenderGui(GuiGraphics guiGraphics) {
		renderSubText(guiGraphics);

		drawStacks(guiGraphics);
		
		drawRandomizerBagHUD(guiGraphics);
	}

	private static final ChatFormatting highlightColor = ChatFormatting.DARK_AQUA;
	private static final ChatFormatting normalColor = ChatFormatting.WHITE;
	private static final Component placingText = Component.literal(
			normalColor + "Left-click to " + highlightColor + "cancel, " +
			normalColor + "Right-click to " + highlightColor + "place");

	private static final Component breakingText = Component.literal(
			normalColor + "Left-click to " + highlightColor + "break, " +
			normalColor + "Right-click to " + highlightColor + "cancel");

	private static void renderSubText(GuiGraphics guiGraphics) {
		var state = SophisticatedBuildingClient.BUILDER_CHAIN.getBuildingState();
		if (state == BuilderChain.BuildingState.IDLE) return;

		var text = state == BuilderChain.BuildingState.PLACING ? placingText : breakingText;

		Minecraft mc = Minecraft.getInstance();
		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();
		var font = mc.font;

		PoseStack ms = guiGraphics.pose();
		ms.pushPose();
		ms.translate(screenWidth / 2.0, screenHeight - 54, 0.0D);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		int l = font.width(text);
		guiGraphics.drawString(font, text, (int)((float)(-l / 2)), -4, 0xffffffff, true);
		RenderSystem.disableBlend();
		ms.popPose();
	}

	//Draw item stacks at cursor, showing what will be used and what is missing
	private static void drawStacks(GuiGraphics guiGraphics) {
		var state = SophisticatedBuildingClient.BUILDER_CHAIN.getBuildingState();
		if (state != BuilderChain.BuildingState.PLACING) return;

		Minecraft mc = Minecraft.getInstance();
		var player = mc.player;
		if (player == null) return;
		
		var stacks = SophisticatedBuildingClient.ITEM_USAGE_TRACKER.total;
		//Show if we are in survival or we are using multiple types of items
		if (player.isCreative() && stacks.size() <= 1) {
			return;
		}

		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();

		int x = screenWidth / 2 + 10;
		int y = screenHeight / 2 - 8;

		//Draw item texture with count
		int i = 0;
		for (var stack : stacks.entrySet()) {
			int total = stack.getValue();
			int missing = SophisticatedBuildingClient.ITEM_USAGE_TRACKER.getMissingCount(stack.getKey());

			if (total - missing > 0) {
				drawItemStack(guiGraphics, new ItemStack(stack.getKey(), total - missing), x + i * 20, y, false);
				i++;
			}

			if (missing > 0) {
				drawItemStack(guiGraphics, new ItemStack(stack.getKey(), missing), x + i * 20, y, true);
				i++;
			}
		}
	}

	private static void drawItemStack(GuiGraphics guiGraphics, ItemStack stack, int x, int y, boolean missing) {
		guiGraphics.renderItem(stack, x, y);

		// Draw count text, red if missing.
		PoseStack ms = guiGraphics.pose();
		ms.pushPose();
		Font font = Minecraft.getInstance().font;
		String text = String.valueOf(stack.getCount());
		ms.translate(0.0D, 0.0D, 200.0F);
		MultiBufferSource.BufferSource multibuffersource$buffersource = MultiBufferSource.immediate(TEXT_BUFFER);
		font.drawInBatch(text, (float)(x + 19 - 2 - font.width(text)), (float)(y + 6 + 3), missing ? ChatFormatting.RED.getColor() : ChatFormatting.WHITE.getColor(), true, ms.last().pose(), multibuffersource$buffersource, Font.DisplayMode.NORMAL, 0, 15728880);
		multibuffersource$buffersource.endBatch();
		ms.popPose();
	}

	protected static VertexConsumer beginLines(MultiBufferSource.BufferSource renderTypeBuffer) {
		return renderTypeBuffer.getBuffer(BuildRenderTypes.LINES);
	}

	protected static void endLines(MultiBufferSource.BufferSource renderTypeBuffer) {
		renderTypeBuffer.endBatch();
	}

	protected static VertexConsumer beginPlanes(MultiBufferSource.BufferSource renderTypeBuffer) {
		return renderTypeBuffer.getBuffer(BuildRenderTypes.PLANES);
	}

	protected static void endPlanes(MultiBufferSource.BufferSource renderTypeBuffer) {
		renderTypeBuffer.endBatch();
	}

	/**
	 * Draw the randomizer bag HUD.
	 */
	private static void drawRandomizerBagHUD(GuiGraphics guiGraphics) {
		var player = Minecraft.getInstance().player;
		if (player == null) return;

		if (player.isCreative()) {
			return;
		}

		ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
		Map<Item, Integer> bundledCounts = new LinkedHashMap<>();

		if (heldItem.getItem() instanceof AbstractRandomizerBagItem bagItem) {
			IItemHandler bagInventory = bagItem.getBagInventory(heldItem);
			if (bagInventory == null) return;

			List<ItemStack> templates = bagItem.getTemplates(bagInventory);
			if (templates.isEmpty()) return;

			// Bundle items by type.
			for (ItemStack template : templates) {
				Item item = template.getItem();
				int inventoryCount = InventoryHelper.findTotalItemsForDisplay(player, item);
				bundledCounts.merge(item, inventoryCount, Integer::max);
			}
		} else if (heldItem.getItem() instanceof net.minecraft.world.item.BlockItem) {
			Item item = heldItem.getItem();
			int inventoryCount = InventoryHelper.findTotalItemsForDisplay(player, item);
			bundledCounts.put(item, inventoryCount);
		} else {
			return;
		}

		if (bundledCounts.isEmpty()) return;

		int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

		int padding = 10;
		float scale = 0.75f; // slightly larger to leave room for counts
		int itemSize = Math.round(16 * scale);
		int spacing = 8; // fixed spacing for clearer separation
		int maxPerRow = 5;
		
		int totalItems = bundledCounts.size();
		int numRows = (int) Math.ceil(totalItems / (double) maxPerRow);
		int itemsInLastRow = totalItems % maxPerRow;
		if (itemsInLastRow == 0 && totalItems > 0) itemsInLastRow = maxPerRow;

		// Draw each template with count
		int i = 0;
		for (var entry : bundledCounts.entrySet()) {
			Item item = entry.getKey();
			int count = entry.getValue();
			
			int row = i / maxPerRow;
			int col = i % maxPerRow;
			
			// Calculate items in this row for right-alignment
			int itemsInThisRow = (row == numRows - 1) ? itemsInLastRow : maxPerRow;
			int rowWidth = itemsInThisRow * (itemSize + spacing) - spacing;
			
			int itemX = screenWidth - padding - rowWidth + col * (itemSize + spacing);
			int itemY = screenHeight - padding - (numRows - row) * (itemSize + spacing);
			
			drawRandomizerHUDItem(guiGraphics, new ItemStack(item), itemX, itemY, count, scale);
			i++;
		}
	}

	// Abbreviate large counts (e.g., 11200 -> 11.2k, 1_000_000 -> 1m)
	private static String formatCount(int count) {
		if (count < 1000) {
			return String.valueOf(count);
		}

		double value;
		String suffix;
		if (count < 1_000_000) {
			value = count / 1000.0;
			suffix = "k";
		} else if (count < 1_000_000_000) {
			value = count / 1_000_000.0;
			suffix = "m";
		} else {
			value = count / 1_000_000_000.0;
			suffix = "b";
		}

		double rounded = Math.round(value * 10.0) / 10.0; // one decimal max
		if (Math.abs(rounded - Math.rint(rounded)) < 1e-9) {
			return String.format(Locale.ROOT, "%.0f%s", rounded, suffix);
		}
		return String.format(Locale.ROOT, "%.1f%s", rounded, suffix);
	}

	/**
	 * Draw a single item stack for the randomizer bag HUD with custom count.
	 */
	private static void drawRandomizerHUDItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int count, float scale) {
		PoseStack ms = guiGraphics.pose();
		ms.pushPose();
		ms.translate(x, y, 0);
		ms.scale(scale, scale, 1.0f);
		
		// Render item at origin (since we've translated)
		guiGraphics.renderItem(stack, 0, 0);

		// Draw count text
		Font font = Minecraft.getInstance().font;
		String text = formatCount(count);
		ms.translate(0.0D, 0.0D, 200.0F);
		// Reuse GUI buffer for text rendering
		MultiBufferSource.BufferSource multibuffersource = MultiBufferSource.immediate(GUI_BUFFER);
		
		// Color based on count: red if 0, yellow if low (<=31), white otherwise (32+)
		int color;
		if (count == 0) {
			color = ChatFormatting.RED.getColor();
		} else if (count <= 31) {
			color = ChatFormatting.YELLOW.getColor();
		} else {
			color = ChatFormatting.WHITE.getColor();
		}
		
		font.drawInBatch(text, (float)(16 - 2 - font.width(text)), (float)(6 + 3), color, true, ms.last().pose(), multibuffersource, Font.DisplayMode.NORMAL, 0, 15728880);
		multibuffersource.endBatch();
		ms.popPose();
	}

}

