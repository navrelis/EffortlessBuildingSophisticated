package sophisticated.building.fabric.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.inventory.ItemStackHandler;
import sophisticated.building.platform.services.IPlatformHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricPlatformHelper implements IPlatformHelper {

    // Per-player state is kept in memory for the session (it is not saved with the player on Fabric)
    private static final Map<UUID, PowerLevel> POWER_LEVELS = new ConcurrentHashMap<>();
    private static final Map<UUID, CompoundTag> PERSISTENT_DATA = new ConcurrentHashMap<>();

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isPhysicalClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public <T extends Item> Supplier<T> registerItem(String path, Function<Item.Properties, T> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SophisticatedBuilding.asResource(path));
        T registered = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
        return () -> registered;
    }

    @Override
    public Supplier<CreativeModeTab> registerCreativeTab(String path, Supplier<CreativeModeTab> tab) {
        CreativeModeTab registered = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SophisticatedBuilding.asResource(path), tab.get());
        return () -> registered;
    }

    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String path, MenuType.MenuSupplier<T> factory) {
        MenuType<T> registered = Registry.register(BuiltInRegistries.MENU, SophisticatedBuilding.asResource(path), new MenuType<>(factory, FeatureFlags.REGISTRY.allFlags()));
        return () -> registered;
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0);
    }

    @Override
    public PowerLevel getPowerLevel(Player player) {
        return POWER_LEVELS.computeIfAbsent(player.getUUID(), key -> new PowerLevel());
    }

    @Override
    public void setPowerLevel(Player player, PowerLevel powerLevel) {
        POWER_LEVELS.put(player.getUUID(), powerLevel);
    }

    @Override
    public boolean hasPowerLevel(Player player) {
        return POWER_LEVELS.containsKey(player.getUUID());
    }

    @Override
    public CompoundTag getPersistentData(Player player) {
        return PERSISTENT_DATA.computeIfAbsent(player.getUUID(), key -> new CompoundTag());
    }

    @Override
    public IItemHandler getBagInventory(ItemStack bag, int size) {
        return new ItemStackHandler.BagItemStackHandler(bag, size);
    }

    @Override
    public void giveItemToPlayer(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        player.getInventory().placeItemBackInInventory(stack);
    }
}
