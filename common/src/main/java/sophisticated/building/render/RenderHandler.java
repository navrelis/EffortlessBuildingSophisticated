package sophisticated.building.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import sophisticated.building.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.create.CreateClient;
import sophisticated.building.create.catnip.animation.AnimationTickHolder;
import sophisticated.building.create.catnip.outliner.Outliner;
import sophisticated.building.create.catnip.render.DefaultSuperRenderTypeBuffer;
import sophisticated.building.create.catnip.render.SuperRenderTypeBuffer;
import sophisticated.building.client.ClientBreakCountdown;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.item.AbstractRandomizerBagItem;
import sophisticated.building.systems.BuilderChain;
import sophisticated.building.utilities.BreakToolHelper;
import sophisticated.building.utilities.InventoryHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main render class for Sophisticated Building. The loader projects call {@link #onRenderWorld} and
 * {@link #onRenderOutlines} from their level render stages and {@link #onRenderGui} from their HUD hook.
 */
public class RenderHandler {

	private static final BufferBuilder LEVEL_BUFFER = new BufferBuilder(1536);
	private static final BufferBuilder GUI_BUFFER = new BufferBuilder(1536);
	private static final BufferBuilder TEXT_BUFFER = new BufferBuilder(1536);

	public static void onRenderWorld(PoseStack ms) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

		MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(LEVEL_BUFFER);

		ms.pushPose();
		ms.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

		//Mirror and radial mirror lines and areas
		ModifierRenderer.render(ms, buffer);

		renderGhostBlocks(ms);

		ms.popPose();

//		renderSubText(ms);
	}

	/**
	 * Render the ghost blocks (block previews) through the Catnip-style layered buffer.
	 */
	private static void renderGhostBlocks(PoseStack ms) {
		SuperRenderTypeBuffer ghostBuffer = DefaultSuperRenderTypeBuffer.getInstance();
		CreateClient.GHOST_BLOCKS.renderAll(ms, ghostBuffer);
		ghostBuffer.draw();
	}

	/**
	 * Render the preview outlines (block clusters, break box). Catnip rendered its outliner itself,
	 * after the translucent blocks on Fabric and after the particles on NeoForge; the loader projects
	 * keep those stages. The pose stack is untranslated, the outlines subtract the camera position.
	 */
	public static void onRenderOutlines(PoseStack ms) {
		Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		float partialTicks = AnimationTickHolder.getPartialTicks();

		ms.pushPose();
		SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
		Outliner.getInstance().renderOutlines(ms, buffer, cameraPos, partialTicks);
		buffer.draw();
		ms.popPose();
	}

	public static void onRenderGui(GuiGraphics guiGraphics) {
		renderSubText(guiGraphics);

		drawStacks(guiGraphics);

		drawBreakCountdown(guiGraphics);

		drawRandomizerBagHUD(guiGraphics);
	}

	private static final Component placingText = new TranslatableComponent("sophisticatedbuilding.hud.placing_hint");

	private static final Component breakingText = new TranslatableComponent("sophisticatedbuilding.hud.breaking_hint");

	private static void renderSubText(GuiGraphics guiGraphics) {
		BuilderChain.BuildingState state = SophisticatedBuildingClient.BUILDER_CHAIN.getBuildingState();
		if (state == BuilderChain.BuildingState.IDLE) return;

		Component text = state == BuilderChain.BuildingState.PLACING ? placingText : breakingText;

		Minecraft mc = Minecraft.getInstance();
		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();
		Font font = mc.font;

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
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) return;

		// Use the pretend state (as if actively building) for the breaking branch so a survival
		// player in e.g. SINGLE mode sees the tool/barrier HUD just from looking at a block - this
		// is how they learn why nothing breaks. The placing branch below keeps using the actual
		// state, unchanged.
		if (SophisticatedBuildingClient.BUILDER_CHAIN.getPretendBuildingState() == BuilderChain.BuildingState.BREAKING) {
			drawBreakPlanStacks(guiGraphics, mc, player);
			return;
		}

		BuilderChain.BuildingState state = SophisticatedBuildingClient.BUILDER_CHAIN.getBuildingState();
		if (state != BuilderChain.BuildingState.PLACING) return;

		Map<Item, Integer> stacks = SophisticatedBuildingClient.ITEM_USAGE_TRACKER.total;
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
		for (Map.Entry<Item, Integer> stack : stacks.entrySet()) {
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

	//Show the survival break plan: the tools that will be used (with counts) and a barrier icon
	//with the count of blocks that cannot be broken.
	private static void drawBreakPlanStacks(GuiGraphics guiGraphics, Minecraft mc, net.minecraft.world.entity.player.Player player) {
		if (player.isCreative()) return;

		BreakToolHelper.BreakPlan plan = SophisticatedBuildingClient.BUILDER_CHAIN.getBreakPlan();
		if (plan == null || (plan.usesPerTool.isEmpty() && plan.unbreakable == 0)) return;

		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();

		int x = screenWidth / 2 + 10;
		int y = screenHeight / 2 - 8;

		int i = 0;
		for (Map.Entry<BreakToolHelper.ToolSlot, Integer> entry : plan.usesPerTool.entrySet()) {
			ItemStack stack = entry.getKey().get().copy();
			stack.setCount(Math.min(entry.getValue(), stack.getMaxStackSize()));
			drawItemStack(guiGraphics, stack, x + i * 20, y, false);
			i++;
		}

		if (plan.unbreakable > 0) {
			ItemStack barrier = new ItemStack(net.minecraft.world.item.Items.BARRIER, Math.min(plan.unbreakable, 99));
			drawItemStack(guiGraphics, barrier, x + i * 20, y, true);
			i++;
		}

		// Show the estimated mining delay next to the tool icons, before the player clicks.
		if (plan.delayTicks > 0) {
			String seconds = String.format(Locale.ROOT, "%.1f", plan.delayTicks / 20f);
			String text = I18n.get("sophisticatedbuilding.hud.break_estimate", seconds);
			Font font = Minecraft.getInstance().font;
			guiGraphics.drawString(font, text, x + i * 20 + 4, y + 4, 0xffffffff, true);
		}
	}

	/**
	 * Draws the on-screen countdown until the current survival break actually applies: centred
	 * text with the block count and remaining seconds, plus a progress bar below it (T-S10).
	 */
	private static void drawBreakCountdown(GuiGraphics guiGraphics) {
		if (!ClientBreakCountdown.hasActive()) return;

		Minecraft mc = Minecraft.getInstance();
		int screenWidth = mc.getWindow().getGuiScaledWidth();
		int screenHeight = mc.getWindow().getGuiScaledHeight();
		Font font = mc.font;

		int remaining = ClientBreakCountdown.remainingTicks();
		int total = ClientBreakCountdown.totalTicks();
		int blockCount = ClientBreakCountdown.totalBlockCount();
		String seconds = String.format(Locale.ROOT, "%.1f", remaining / 20f);
		String key = ClientBreakCountdown.onlyPlacing() ? "sophisticatedbuilding.hud.replace_countdown" : "sophisticatedbuilding.hud.break_countdown";
		String text = I18n.get(key, blockCount, seconds);

		int textX = screenWidth / 2;
		int textY = screenHeight / 2 + 24;
		guiGraphics.drawCenteredString(font, text, textX, textY, 0xffffffff);

		int barWidth = 100;
		int barHeight = 4;
		int barX = screenWidth / 2 - barWidth / 2;
		int barY = textY + font.lineHeight + 2;
		float progress = total > 0 ? Math.max(0f, Math.min(1f, 1f - (remaining / (float) total))) : 0f;
		int filledWidth = Math.round(barWidth * progress);

		guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xAA000000);
		if (filledWidth > 0) {
			guiGraphics.fill(barX, barY, barX + filledWidth, barY + barHeight, 0xFFDD3333);
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
		font.drawInBatch(text, (float)(x + 19 - 2 - font.width(text)), (float)(y + 6 + 3), missing ? ChatFormatting.RED.getColor() : ChatFormatting.WHITE.getColor(), true, ms.last().pose(), multibuffersource$buffersource, false, 0, 15728880);
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
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) return;

		if (player.isCreative()) {
			return;
		}

		ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
		Map<Item, Integer> bundledCounts = new LinkedHashMap<>();

		if (heldItem.getItem() instanceof AbstractRandomizerBagItem) {
			AbstractRandomizerBagItem bagItem = (AbstractRandomizerBagItem) heldItem.getItem();
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
		for (Map.Entry<Item, Integer> entry : bundledCounts.entrySet()) {
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
		
		font.drawInBatch(text, (float)(16 - 2 - font.width(text)), (float)(6 + 3), color, true, ms.last().pose(), multibuffersource, false, 0, 15728880);
		multibuffersource.endBatch();
		ms.popPose();
	}

}

