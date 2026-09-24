package sophisticated.building.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;

import java.util.HashMap;
import java.util.Map;

public class MaterialCostOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Only show if a build mode is active
        if (SophisticatedBuildingClient.BUILD_MODES.getBuildMode() == BuildModeEnum.DISABLED) return;

        BlockSet blocks = SophisticatedBuildingClient.BUILDER_CHAIN.getBlocks();
        if (blocks == null || blocks.isEmpty()) return;

        // Calculate material costs
        Map<Item, Integer> costs = new HashMap<>();
        int totalBlocks = 0;

        for (BlockEntry entry : blocks) {
            if (entry.newBlockState != null && !entry.newBlockState.isAir()) {
                Item item = entry.newBlockState.getBlock().asItem();
                if (item != null) {
                    costs.merge(item, 1, Integer::sum);
                    totalBlocks++;
                }
            }
        }

        if (costs.isEmpty()) return;

        // Render HUD
        int x = 10;
        int y = mc.getWindow().getGuiScaledHeight() / 2 - (costs.size() * 20) / 2;
        
        // Draw background
        // guiGraphics.fill(x - 5, y - 5, x + 100, y + costs.size() * 20 + 5, 0x90000000);

        for (Map.Entry<Item, Integer> entry : costs.entrySet()) {
            ItemStack stack = new ItemStack(entry.getKey());
            int count = entry.getValue();

            guiGraphics.renderItem(stack, x, y);
            guiGraphics.renderItemDecorations(mc.font, stack, x, y);
            guiGraphics.drawString(mc.font, count + "x " + stack.getHoverName().getString(), x + 20, y + 4, 0xFFFFFF);

            y += 20;
        }
    }
}
