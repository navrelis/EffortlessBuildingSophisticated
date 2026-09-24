package sophisticated.building.forge.platform;

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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.DeferredRegister;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.forge.PowerLevelCapability;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.inventory.ItemStackHandler;
import sophisticated.building.platform.services.IPlatformHelper;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ForgePlatformHelper implements IPlatformHelper {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, SophisticatedBuilding.MODID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, SophisticatedBuilding.MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SophisticatedBuilding.MODID);

    /** Registers the deferred registers filled by {@code SophisticatedBuilding}'s initialisation. */
    public static void registerDeferredRegisters(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CONTAINERS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
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
    public <T extends Item> Supplier<T> registerItem(String path, Function<Item.Properties, T> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SophisticatedBuilding.asResource(path));
        return ITEMS.register(path, () -> factory.apply(new Item.Properties().setId(key)));
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
     * The bag's {@code minecraft:container} component, read and written directly (Forge has no
     * component-backed item handler like NeoForge's {@code ComponentItemHandler}).
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
