package sophisticated.building.forge.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.forge.PowerLevelCapability;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.inventory.ItemStackHandler;
import sophisticated.building.platform.services.IPlatformHelper;

import java.util.function.Supplier;

public final class ForgePlatformHelper implements IPlatformHelper {

    // Forge 1.17.1 creates deferred registers from its own registries only (registry keys from Forge 40 for 1.18.2)
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SophisticatedBuilding.MODID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, SophisticatedBuilding.MODID);

    /** Registers the deferred registers filled by {@code SophisticatedBuilding}'s initialisation. */
    public static void registerDeferredRegisters(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CONTAINERS.register(modEventBus);
    }

    @Override
    public String getPlatformName() {
        return "Forge";
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
    public <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerMenu(String path, MenuType.MenuSupplier<T> factory) {
        return CONTAINERS.register(path, () -> new MenuType<>(factory));
    }

    @Override
    public CreativeModeTab createCreativeTab(String path, Supplier<ItemStack> icon) {
        // Forge's label constructor appends the tab to the vanilla tab array
        return new CreativeModeTab(SophisticatedBuilding.MODID + "." + path) {
            @Override
            public ItemStack makeIcon() {
                return icon.get();
            }
        };
    }

    @Override
    public PowerLevel getPowerLevel(Player player) {
        return PowerLevelCapability.get(player);
    }

    @Override
    public void setPowerLevel(Player player, PowerLevel powerLevel) {
        PowerLevelCapability.set(player, powerLevel);
    }

    @Override
    public boolean hasPowerLevel(Player player) {
        return PowerLevelCapability.has(player);
    }

    @Override
    public CompoundTag getPersistentData(Player player) {
        return player.getPersistentData();
    }

    /**
     * The bag's {@code Items} tag, read and written directly (the mod gives its bags no item handler
     * capability on Forge, as on Fabric).
     */
    @Override
    public IItemHandler getBagInventory(ItemStack bag, int size) {
        return new ItemStackHandler.BagItemStackHandler(bag, size);
    }

    @Override
    public void giveItemToPlayer(Player player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }
}
