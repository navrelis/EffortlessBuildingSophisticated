package sophisticated.building.platform.services;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.inventory.IItemHandler;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Loader information, content registration and per-player data.
 */
public interface IPlatformHelper {

    /** Name of the running mod loader, for logs. */
    String getPlatformName();

    boolean isModLoaded(String modId);

    /** True on the physical client (also while it hosts a singleplayer world). */
    boolean isPhysicalClient();

    /**
     * Registers an item. Fabric registers immediately; NeoForge defers to its registry event, so the
     * item may only be read through the returned supplier after registration.
     */
    <T extends Item> Supplier<T> registerItem(String path, Supplier<T> item);

    Supplier<CreativeModeTab> registerCreativeTab(String path, Supplier<CreativeModeTab> tab);

    <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String path, MenuType.MenuSupplier<T> factory);

    /** A creative tab builder the loader positions itself. */
    CreativeModeTab.Builder creativeTabBuilder();

    /**
     * The player's power level, created with defaults on first access. On NeoForge this is the saved
     * {@code power_level} attachment; on Fabric it is kept in memory for the session.
     */
    PowerLevel getPowerLevel(Player player);

    void setPowerLevel(Player player, PowerLevel powerLevel);

    boolean hasPowerLevel(Player player);

    /**
     * Mutable per-player data for the build state and the modifier settings. On NeoForge this is the
     * player's persistent data (saved with the player); on Fabric it is kept in memory for the session.
     */
    CompoundTag getPersistentData(Player player);

    /**
     * The item inventory of a randomizer bag stack (its {@code Items} tag), or null
     * if the stack has none. NeoForge goes through the item handler capability registered for the bags.
     */
    @Nullable
    IItemHandler getBagInventory(ItemStack bag, int size);

    /**
     * Puts {@code stack} into the player's inventory and drops what does not fit. NeoForge uses its
     * item handler helper (also plays the pickup sound), Fabric the vanilla inventory method.
     */
    void giveItemToPlayer(Player player, ItemStack stack);
}
