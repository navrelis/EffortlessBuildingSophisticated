package sophisticated.building.neoforge.platform;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.DeferredRegister;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.neoforge.NeoForgeAttachments;
import sophisticated.building.platform.services.IPlatformHelper;

import java.util.function.Supplier;

public final class NeoForgePlatformHelper implements IPlatformHelper {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SophisticatedBuilding.MODID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(BuiltInRegistries.MENU, SophisticatedBuilding.MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SophisticatedBuilding.MODID);

    /** Registers the deferred registers filled by {@code SophisticatedBuilding}'s initialisation. */
    public static void registerDeferredRegisters(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CONTAINERS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        try {
            // ModList is available after mod discovery; fall back to the loading mod list before that
            var modList = ModList.get();
            if (modList != null) {
                return modList.isLoaded(modId);
            }
            return FMLLoader.getLoadingModList().getModFileById(modId) != null;
        } catch (Exception e) {
            // If anything fails, assume not loaded
            return false;
        }
    }

    @Override
    public boolean isPhysicalClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public <T extends Item> Supplier<T> registerItem(String path, Supplier<T> item) {
        return ITEMS.register(path, item);
    }

    @Override
    public Supplier<CreativeModeTab> registerCreativeTab(String path, Supplier<CreativeModeTab> tab) {
        return CREATIVE_MODE_TABS.register(path, tab);
    }

    @Override
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String path, MenuType.MenuSupplier<T> factory) {
        return CONTAINERS.register(path, () -> new MenuType<>(factory, FeatureFlags.REGISTRY.allFlags()));
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

    @Override
    public PowerLevel getPowerLevel(Player player) {
        return player.getData(NeoForgeAttachments.POWER_LEVEL);
    }

    @Override
    public void setPowerLevel(Player player, PowerLevel powerLevel) {
        player.setData(NeoForgeAttachments.POWER_LEVEL, powerLevel);
    }

    @Override
    public boolean hasPowerLevel(Player player) {
        return player.hasData(NeoForgeAttachments.POWER_LEVEL);
    }

    @Override
    public CompoundTag getPersistentData(Player player) {
        return player.getPersistentData();
    }

    @Override
    public IItemHandler getBagInventory(ItemStack bag, int size) {
        var handler = bag.getCapability(Capabilities.ItemHandler.ITEM, null);
        return handler == null ? null : new NeoForgeItemHandler(handler);
    }

    @Override
    public void giveItemToPlayer(Player player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }

    /** Our item handler view of a NeoForge item handler (the bags' {@code BagItemHandler}). */
    private record NeoForgeItemHandler(net.neoforged.neoforge.items.IItemHandler handler) implements IItemHandler {
        @Override
        public int getSlots() {
            return handler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return handler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return handler.getSlotLimit(slot);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            ((IItemHandlerModifiable) handler).setStackInSlot(slot, stack);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return handler.isItemValid(slot, stack);
        }
    }
}
