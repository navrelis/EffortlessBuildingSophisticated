package sophisticated.building;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.gui.DiamondRandomizerBagContainer;
import sophisticated.building.gui.GoldenRandomizerBagContainer;
import sophisticated.building.gui.OmegaRandomizerBagContainer;
import sophisticated.building.gui.RandomizerBagContainer;
import sophisticated.building.item.CompressedBlockItem;
import sophisticated.building.item.DiamondRandomizerBagItem;
import sophisticated.building.item.GoldenRandomizerBagItem;
import sophisticated.building.item.OmegaRandomizerBagItem;
import sophisticated.building.item.RandomizerBagItem;
import sophisticated.building.item.ReachUpgrade1Item;
import sophisticated.building.item.ReachUpgrade2Item;
import sophisticated.building.item.ReachUpgrade3Item;
import sophisticated.building.proxy.ClientProxy;
import sophisticated.building.proxy.ServerProxy;
import sophisticated.building.systems.ItemUsageTracker;
import sophisticated.building.systems.ServerBlockPlacer;
import sophisticated.building.systems.UndoRedo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SophisticatedBuilding {

    public static final String MODID = "sophisticatedbuilding";
    public static final Logger logger = LogManager.getLogger();

    public static final ServerBlockPlacer SERVER_BLOCK_PLACER = new ServerBlockPlacer();
    public static final UndoRedo UNDO_REDO = new UndoRedo();
    public static final ItemUsageTracker ITEM_USAGE_TRACKER = new ItemUsageTracker();

    public static final Supplier<RandomizerBagItem> RANDOMIZER_BAG_ITEM = registerItem("randomizer_bag", RandomizerBagItem::new);
    public static final Supplier<GoldenRandomizerBagItem> GOLDEN_RANDOMIZER_BAG_ITEM = registerItem("golden_randomizer_bag", GoldenRandomizerBagItem::new);
    public static final Supplier<DiamondRandomizerBagItem> DIAMOND_RANDOMIZER_BAG_ITEM = registerItem("diamond_randomizer_bag", DiamondRandomizerBagItem::new);
    public static final Supplier<OmegaRandomizerBagItem> OMEGA_RANDOMIZER_BAG_ITEM = registerItem("omega_randomizer_bag", OmegaRandomizerBagItem::new);
    public static final Supplier<ReachUpgrade1Item> REACH_UPGRADE_1_ITEM = registerItem("reach_upgrade1", ReachUpgrade1Item::new);
    public static final Supplier<ReachUpgrade2Item> REACH_UPGRADE_2_ITEM = registerItem("reach_upgrade2", ReachUpgrade2Item::new);
    public static final Supplier<ReachUpgrade3Item> REACH_UPGRADE_3_ITEM = registerItem("reach_upgrade3", ReachUpgrade3Item::new);

    public static final Supplier<CompressedBlockItem> COMPRESSED_DIRT = registerItem("compressed_dirt", CompressedBlockItem::new);
    public static final Supplier<CompressedBlockItem> COMPRESSED_COBBLESTONE = registerItem("compressed_cobblestone", CompressedBlockItem::new);
    public static final Supplier<CompressedBlockItem> COMPRESSED_SAND = registerItem("compressed_sand", CompressedBlockItem::new);
    public static final Supplier<CompressedBlockItem> COMPRESSED_COBBLED_DEEPSLATE = registerItem("compressed_cobbled_deepslate", CompressedBlockItem::new);

    public static final Supplier<Item> BUILDING_UPGRADE_1 = registerItem("building_upgrade_1", () -> createBuildingUpgrade(1, 32));
    public static final Supplier<Item> BUILDING_UPGRADE_2 = registerItem("building_upgrade_2", () -> createBuildingUpgrade(2, 64));
    public static final Supplier<Item> BUILDING_UPGRADE_3 = registerItem("building_upgrade_3", () -> createBuildingUpgrade(3, 128));
    public static final Supplier<Item> BUILDING_UPGRADE_4 = registerItem("building_upgrade_4", () -> createBuildingUpgrade(4, 256));
    public static final Supplier<Item> BUILDING_UPGRADE_OMEGA = registerItem("building_upgrade_omega", () -> createBuildingUpgrade(5, 2048));

    private static final List<Supplier<Item>> BUILDING_UPGRADE_ITEMS = new ArrayList<>();

    static {
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_1);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_2);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_3);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_4);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_OMEGA);
    }

    public static final Supplier<CreativeModeTab> CREATIVE_TAB = registerCreativeTab("main", () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .icon(() -> new ItemStack(BUILDING_UPGRADE_OMEGA.get()))
                    .title(Component.translatable("itemGroup.sophisticatedbuilding"))
                    .displayItems((parameters, output) -> {
                        output.accept(RANDOMIZER_BAG_ITEM.get());
                        output.accept(GOLDEN_RANDOMIZER_BAG_ITEM.get());
                        output.accept(DIAMOND_RANDOMIZER_BAG_ITEM.get());
                        output.accept(OMEGA_RANDOMIZER_BAG_ITEM.get());
                        output.accept(REACH_UPGRADE_1_ITEM.get());
                        output.accept(REACH_UPGRADE_2_ITEM.get());
                        output.accept(REACH_UPGRADE_3_ITEM.get());
                        output.accept(COMPRESSED_DIRT.get());
                        output.accept(COMPRESSED_COBBLESTONE.get());
                        output.accept(COMPRESSED_SAND.get());
                        output.accept(COMPRESSED_COBBLED_DEEPSLATE.get());
                        for (Supplier<Item> upgradeItem : BUILDING_UPGRADE_ITEMS) {
                            output.accept(upgradeItem.get());
                        }
                    })
                    .build());

    public static final Supplier<MenuType<RandomizerBagContainer>> RANDOMIZER_BAG_CONTAINER = registerContainer("randomizer_bag", RandomizerBagContainer::new);
    public static final Supplier<MenuType<GoldenRandomizerBagContainer>> GOLDEN_RANDOMIZER_BAG_CONTAINER = registerContainer("golden_randomizer_bag", GoldenRandomizerBagContainer::new);
    public static final Supplier<MenuType<DiamondRandomizerBagContainer>> DIAMOND_RANDOMIZER_BAG_CONTAINER = registerContainer("diamond_randomizer_bag", DiamondRandomizerBagContainer::new);
    public static final Supplier<MenuType<OmegaRandomizerBagContainer>> OMEGA_RANDOMIZER_BAG_CONTAINER = registerContainer("omega_randomizer_bag", OmegaRandomizerBagContainer::new);

    private static boolean initialized;
    private static boolean backpacksUpgradeContainersRegistered;

    private SophisticatedBuilding() {
    }

    public static void initializeCommon() {
        if (initialized) {
            return;
        }
        initialized = true;

        CompatHelper.setup();
    }

    public static void registerBackpacksUpgradeContainers() {
        if (backpacksUpgradeContainersRegistered || !CompatHelper.isSophisticatedBackpacksLoaded()) {
            return;
        }

        try {
            Class<?> integrationClass = Class.forName("sophisticated.building.integration.SophisticatedBackpacksIntegration");
            Method registerMethod = integrationClass.getMethod("registerUpgradeContainers", Item[].class);
            Item[] upgradeItems = new Item[] {
                    BUILDING_UPGRADE_1.get(),
                    BUILDING_UPGRADE_2.get(),
                    BUILDING_UPGRADE_3.get(),
                    BUILDING_UPGRADE_4.get(),
                    BUILDING_UPGRADE_OMEGA.get()
            };
            registerMethod.invoke(null, (Object) upgradeItems);
            backpacksUpgradeContainersRegistered = true;
            logger.info("Registered Sophisticated Backpacks upgrade containers");
        } catch (Throwable t) {
            logger.warn("Failed to register building upgrade containers: {}", t.toString());
        }
    }

    private static <T extends Item> Supplier<T> registerItem(String path, Supplier<T> factory) {
        T item = Registry.register(BuiltInRegistries.ITEM, asResource(path), factory.get());
        return () -> item;
    }

    private static Supplier<CreativeModeTab> registerCreativeTab(String path, Supplier<CreativeModeTab> factory) {
        CreativeModeTab tab = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, asResource(path), factory.get());
        return () -> tab;
    }

    private static <T extends AbstractContainerMenu> Supplier<MenuType<T>> registerContainer(String path, MenuType.MenuSupplier<T> factory) {
        MenuType<T> menuType = Registry.register(BuiltInRegistries.MENU, asResource(path), new MenuType<>(factory, FeatureFlags.REGISTRY.allFlags()));
        return () -> menuType;
    }

    private static Item createBuildingUpgrade(int tier, int maxBlocks) {
        if (CompatHelper.isSophisticatedBackpacksLoaded()) {
            try {
                Class<?> integrationClass = Class.forName("sophisticated.building.integration.SophisticatedBackpacksIntegration");
                Method createMethod = integrationClass.getMethod("createBuildingUpgrade", int.class, int.class);
                Object createdUpgrade = createMethod.invoke(null, tier, maxBlocks);
                if (createdUpgrade instanceof Item item) {
                    return item;
                }
                logger.warn("Failed to create BuildingUpgradeItem: integration returned unexpected type {}",
                        createdUpgrade == null ? "null" : createdUpgrade.getClass().getName());
            } catch (Throwable t) {
                logger.warn("Failed to create BuildingUpgradeItem, SophisticatedBackpacks may not be loaded properly: {}", t.toString());
            }
        }
        return new Item(new Item.Properties().stacksTo(1));
    }

    public static void log(String msg) {
        logger.info(msg);
    }

    public static void log(Player player, String msg) {
        log(player, msg, false);
    }

    public static void log(Player player, String msg, boolean actionBar) {
        player.displayClientMessage(Component.literal(msg), actionBar);
    }

    public static void logTranslate(Player player, String prefix, String translationKey, String suffix, boolean actionBar) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientProxy.logTranslate(player, prefix, translationKey, suffix, actionBar);
        } else {
            ServerProxy.logTranslate(player, prefix, translationKey, suffix, actionBar);
        }
    }

    public static void logError(String msg) {
        logger.error(msg);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
