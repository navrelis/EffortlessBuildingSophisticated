package sophisticated.building.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.utilities.MaterialCost;
import sophisticated.building.utilities.BlockSet;

import java.util.Map;

/**
 * HUD list of the blocks the current build-mode preview would place. Drawn by the loader projects above the
 * crosshair (NeoForge: a GUI overlay; Fabric: the HUD render callback).
 */
public class MaterialCostOverlay {

    public void render(GuiGraphics guiGraphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Only show if a build mode is active
        if (SophisticatedBuildingClient.BUILD_MODES.getBuildMode() == BuildModeEnum.DISABLED) return;

        BlockSet blocks = SophisticatedBuildingClient.BUILDER_CHAIN.getBlocks();
        if (blocks == null || blocks.isEmpty()) return;

        // Calculate material costs (what the server charges: all items of a state, a merge only the added one)
        Map<Item, Integer> costs = MaterialCost.tally(blocks);
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
