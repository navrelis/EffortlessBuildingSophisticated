package sophisticated.building.fabric.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.fabric.FabricPlayerData;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.inventory.ItemStackHandler;
import sophisticated.building.platform.services.IPlatformHelper;

import java.util.function.Supplier;

public final class FabricPlatformHelper implements IPlatformHelper {

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
    public <T extends Item> Supplier<T> registerItem(String path, Supplier<T> item) {
        T registered = Registry.register(Registry.ITEM, SophisticatedBuilding.asResource(path), item.get());
        return () -> registered;
    }

    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String path, MenuType.MenuSupplier<T> factory) {
        MenuType<T> registered = Registry.register(Registry.MENU, SophisticatedBuilding.asResource(path), new MenuType<>(factory));
        return () -> registered;
    }

    @Override
    public CreativeModeTab createCreativeTab(String path, Supplier<ItemStack> icon) {
        return FabricItemGroupBuilder.create(SophisticatedBuilding.asResource(path)).icon(icon).build();
    }

    @Override
    public PowerLevel getPowerLevel(Player player) {
        return FabricPlayerData.getOrCreatePowerLevel(player);
    }

    @Override
    public void setPowerLevel(Player player, PowerLevel powerLevel) {
        FabricPlayerData.of(player).sophisticatedbuilding$setPowerLevel(powerLevel);
    }

    @Override
    public boolean hasPowerLevel(Player player) {
        return FabricPlayerData.of(player).sophisticatedbuilding$getPowerLevel() != null;
    }

    @Override
    public CompoundTag getPersistentData(Player player) {
        return FabricPlayerData.of(player).sophisticatedbuilding$getData();
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
