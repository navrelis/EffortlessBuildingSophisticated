package sophisticated.building.platform.services;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.utilities.BreakToolHelper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Sophisticated Backpacks integration (Building Upgrade items, backpack block supply, Tool Swapper
 * tools). The loader projects implement it against the Sophisticated Backpacks/Core build of their
 * loader; {@link #NONE} is used when Sophisticated Backpacks is absent or a loader build ships no
 * implementation. Only called after {@code CompatHelper.isSophisticatedBackpacksLoaded()} and
 * inside {@code catch (Exception | LinkageError)} guards, like the direct calls it replaces.
 */
public interface IBackpackIntegration {

    IBackpackIntegration NONE = new IBackpackIntegration() {
    };

    /** The Building Upgrade item for one tier, or null to register the plain placeholder item. */
    @Nullable
    default Item createBuildingUpgrade(int tier, int maxBlocks) {
        return null;
    }

    /** Registers the backpack upgrade container types for the Building Upgrade items. */
    default void registerUpgradeContainers(Item... upgradeItems) {
    }

    /** Client only: registers the Building Upgrade settings tab of the backpack screen. */
    default void registerUpgradeTab() {
    }

    /** Tier (1-5) of the best enabled Building Upgrade the player carries, 0 if none. Server only. */
    default int getBuildingUpgradeTier(Player player) {
        return 0;
    }

    /** Max blocks of the best enabled Building Upgrade the player carries, 0 if none. Server only. */
    default int getMaxBlocksForPlayer(Player player) {
        return 0;
    }

    /** Max blocks that may be pulled from backpacks for {@code itemStack}, 0 without an upgrade. Server only. */
    default int getEffectiveMaxBlocksForPlayer(Player player, ItemStack itemStack) {
        return 0;
    }

    /** Unclamped count of {@code blockItem} across every backpack with an enabled upgrade. Server only. */
    default int countBlockInBackpacksForDisplay(Player player, ItemStack blockItem) {
        return 0;
    }

    /** Extracts up to {@code amount} of {@code blockItem} from the player's upgraded backpacks. Server only. */
    default ItemStack extractBlockFromBackpack(Player player, ItemStack blockItem, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    /** Tools inside enabled Tool Swapper upgrades of the player's backpacks. Server only. */
    default List<BreakToolHelper.ToolSlot> collectBackpackTools(Player player) {
        return List.of();
    }
}
