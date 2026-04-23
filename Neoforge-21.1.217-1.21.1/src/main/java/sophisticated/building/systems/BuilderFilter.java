package sophisticated.building.systems;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;
import sophisticated.building.utilities.PlaceChecker;

@OnlyIn(Dist.CLIENT)
public class BuilderFilter {
    public void filterOnCoordinates(BlockSet blocks, Player player) {
        var world = player.level();
        var iter = blocks.entrySet().iterator();
        while (iter.hasNext()) {
            var pos = iter.next().getValue().blockPos;
            boolean remove = false;

            if (!world.isLoaded(pos)) remove = true;
            if (!world.getWorldBorder().isWithinBounds(pos)) remove = true;

            if (remove) iter.remove();
        }
    }

    public void filterOnExistingBlockStates(BlockSet blocks, Player player) {
        var buildSettings = SophisticatedBuildingClient.BUILD_SETTINGS;
        var buildingState = SophisticatedBuildingClient.BUILDER_CHAIN.getPretendBuildingState();
        boolean placing = buildingState == BuilderChain.BuildingState.PLACING;

        var iter = blocks.entrySet().iterator();
        while (iter.hasNext()) {
            var blockEntry = iter.next().getValue();
            var blockState = blockEntry.existingBlockState;
            boolean remove = false;

            if (buildSettings.shouldProtectTileEntities() && blockState.hasBlockEntity()) remove = true;

            if (placing && !buildSettings.shouldReplaceFiltered()) {
                if (!buildSettings.shouldReplaceAir() && blockState.isAir()) remove = true;
                boolean isReplaceable = blockState.canBeReplaced();
                if (!buildSettings.shouldReplaceBlocks() && !isReplaceable) remove = true;
            }

            if (buildSettings.shouldReplaceFiltered()) {
                var offhandItem = player.getOffhandItem();
                if (!CompatHelper.containsBlock(offhandItem, blockState.getBlock())) remove = true;
            }

            if (remove) iter.remove();
        }
    }

    //Returns true if we should remove the entry
    public boolean filterOnNewBlockState(BlockEntry blockEntry, Player player) {
        var buildingState = SophisticatedBuildingClient.BUILDER_CHAIN.getPretendBuildingState();
        boolean placing = buildingState == BuilderChain.BuildingState.PLACING;

        boolean remove = false;

        if (placing && !PlaceChecker.shouldPlaceBlock(player.level(), blockEntry)) remove = true;

        return remove;
    }
}
