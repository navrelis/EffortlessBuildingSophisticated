package sophisticated.building.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.utilities.MaterialCost;
import sophisticated.building.utilities.BlockSet;

import java.util.Map;

/**
 * HUD layer listing the blocks the current build will use. Registered by each loader project as its HUD layer (vanilla
 * removed {@code LayeredDraw} in 1.21.6).
 */
public class MaterialCostOverlay {

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
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

            guiGraphics.item(stack, x, y);
            guiGraphics.itemDecorations(mc.font, stack, x, y);
            guiGraphics.text(mc.font, count + "x " + stack.getHoverName().getString(), x + 20, y + 4, 0xFFFFFFFF);

            y += 20;
        }
    }
}
