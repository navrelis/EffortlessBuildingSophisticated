package sophisticated.building.systems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;
import sophisticated.building.utilities.PlaceChecker;
import java.util.Iterator;
import java.util.Map;

public class BuilderFilter {
    public void filterOnCoordinates(BlockSet blocks, Player player) {
        Level world = player.level;
        Iterator<Map.Entry<BlockPos, BlockEntry>> iter = blocks.entrySet().iterator();
        while (iter.hasNext()) {
            BlockPos pos = iter.next().getValue().blockPos;
            boolean remove = false;

            if (!world.isLoaded(pos)) remove = true;
            if (!world.getWorldBorder().isWithinBounds(pos)) remove = true;

            if (remove) iter.remove();
        }
    }

    public void filterOnExistingBlockStates(BlockSet blocks, Player player) {
        BuildSettings buildSettings = SophisticatedBuildingClient.BUILD_SETTINGS;
        BuilderChain.BuildingState buildingState = SophisticatedBuildingClient.BUILDER_CHAIN.getPretendBuildingState();
        boolean placing = buildingState == BuilderChain.BuildingState.PLACING;

        Iterator<Map.Entry<BlockPos, BlockEntry>> iter = blocks.entrySet().iterator();
        while (iter.hasNext()) {
            BlockEntry blockEntry = iter.next().getValue();
            BlockState blockState = blockEntry.existingBlockState;
            boolean remove = false;

            if (buildSettings.shouldProtectTileEntities() && blockState.hasBlockEntity()) remove = true;

            if (placing && !buildSettings.shouldReplaceFiltered()) {
                if (!buildSettings.shouldReplaceAir() && blockState.isAir()) remove = true;
                boolean isReplaceable = blockState.getMaterial().isReplaceable();
                if (!buildSettings.shouldReplaceBlocks() && !isReplaceable) remove = true;
            }

            if (buildSettings.shouldReplaceFiltered()) {
                ItemStack offhandItem = player.getOffhandItem();
                if (!CompatHelper.containsBlock(offhandItem, blockState.getBlock())) remove = true;
            }

            if (remove) iter.remove();
        }
    }

    //Returns true if we should remove the entry
    public boolean filterOnNewBlockState(BlockEntry blockEntry, Player player) {
        BuilderChain.BuildingState buildingState = SophisticatedBuildingClient.BUILDER_CHAIN.getPretendBuildingState();
        boolean placing = buildingState == BuilderChain.BuildingState.PLACING;

        boolean remove = false;

        if (placing && !PlaceChecker.shouldPlaceBlock(player.level, blockEntry)) remove = true;

        return remove;
    }
}

