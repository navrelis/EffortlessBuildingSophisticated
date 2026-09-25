package sophisticated.building;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
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
import sophisticated.building.platform.Services;
import sophisticated.building.proxy.ClientProxy;
import sophisticated.building.proxy.ServerProxy;
import sophisticated.building.systems.ItemUsageTracker;
import sophisticated.building.systems.ServerBlockPlacer;
import sophisticated.building.systems.UndoRedo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Loader-neutral core of the mod: its content (registered through the platform services when this
 * class is initialised, see {@link #init()}) and the server systems. The loader projects hold the
 * entry points.
 */
public final class SophisticatedBuilding {

    public static final String MODID = "sophisticatedbuilding";
    public static final Logger logger = LogManager.getLogger();

    public static final ServerBlockPlacer SERVER_BLOCK_PLACER = new ServerBlockPlacer();
    public static final UndoRedo UNDO_REDO = new UndoRedo();
    public static final ItemUsageTracker ITEM_USAGE_TRACKER = new ItemUsageTracker();

    public static final Supplier<RandomizerBagItem> RANDOMIZER_BAG_ITEM = Services.PLATFORM.registerItem("randomizer_bag", RandomizerBagItem::new);
    public static final Supplier<GoldenRandomizerBagItem> GOLDEN_RANDOMIZER_BAG_ITEM = Services.PLATFORM.registerItem("golden_randomizer_bag", GoldenRandomizerBagItem::new);
    public static final Supplier<DiamondRandomizerBagItem> DIAMOND_RANDOMIZER_BAG_ITEM = Services.PLATFORM.registerItem("diamond_randomizer_bag", DiamondRandomizerBagItem::new);
    public static final Supplier<OmegaRandomizerBagItem> OMEGA_RANDOMIZER_BAG_ITEM = Services.PLATFORM.registerItem("omega_randomizer_bag", OmegaRandomizerBagItem::new);
    public static final Supplier<ReachUpgrade1Item> REACH_UPGRADE_1_ITEM = Services.PLATFORM.registerItem("reach_upgrade1", ReachUpgrade1Item::new);
    public static final Supplier<ReachUpgrade2Item> REACH_UPGRADE_2_ITEM = Services.PLATFORM.registerItem("reach_upgrade2", ReachUpgrade2Item::new);
    public static final Supplier<ReachUpgrade3Item> REACH_UPGRADE_3_ITEM = Services.PLATFORM.registerItem("reach_upgrade3", ReachUpgrade3Item::new);

    // Compressed block items (crafting materials for building upgrades)
    public static final Supplier<CompressedBlockItem> COMPRESSED_DIRT = Services.PLATFORM.registerItem("compressed_dirt", CompressedBlockItem::new);
    public static final Supplier<CompressedBlockItem> COMPRESSED_COBBLESTONE = Services.PLATFORM.registerItem("compressed_cobblestone", CompressedBlockItem::new);
    public static final Supplier<CompressedBlockItem> COMPRESSED_SAND = Services.PLATFORM.registerItem("compressed_sand", CompressedBlockItem::new);
    public static final Supplier<CompressedBlockItem> COMPRESSED_COBBLED_DEEPSLATE = Services.PLATFORM.registerItem("compressed_cobbled_deepslate", CompressedBlockItem::new);

    // Building upgrade items (backpack upgrades). Created through the backpack integration so that
    // Sophisticated Core classes are never loaded when Sophisticated Backpacks is absent; placeholder
    // items otherwise. Tier 1: 32 blocks, Tier 2: 64, Tier 3: 128, Tier 4: 256, Omega: 2048
    public static final Supplier<Item> BUILDING_UPGRADE_1 = Services.PLATFORM.registerItem("building_upgrade_1", properties -> createBuildingUpgrade(properties, 1, 32));
    public static final Supplier<Item> BUILDING_UPGRADE_2 = Services.PLATFORM.registerItem("building_upgrade_2", properties -> createBuildingUpgrade(properties, 2, 64));
    public static final Supplier<Item> BUILDING_UPGRADE_3 = Services.PLATFORM.registerItem("building_upgrade_3", properties -> createBuildingUpgrade(properties, 3, 128));
    public static final Supplier<Item> BUILDING_UPGRADE_4 = Services.PLATFORM.registerItem("building_upgrade_4", properties -> createBuildingUpgrade(properties, 4, 256));
    public static final Supplier<Item> BUILDING_UPGRADE_OMEGA = Services.PLATFORM.registerItem("building_upgrade_omega", properties -> createBuildingUpgrade(properties, 5, 2048));

    private static final List<Supplier<Item>> BUILDING_UPGRADE_ITEMS = new ArrayList<>();

    static {
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_1);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_2);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_3);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_4);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_OMEGA);
    }

    public static final Supplier<CreativeModeTab> CREATIVE_TAB = Services.PLATFORM.registerCreativeTab("main", () ->
            Services.PLATFORM.creativeTabBuilder()
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
                        // Building upgrades (always shown - they are placeholder items when SB is not loaded)
                        for (Supplier<Item> upgradeItem : BUILDING_UPGRADE_ITEMS) {
                            output.accept(upgradeItem.get());
                        }
                    })
                    .build());

    public static final Supplier<MenuType<RandomizerBagContainer>> RANDOMIZER_BAG_CONTAINER = Services.PLATFORM.registerMenu("randomizer_bag", RandomizerBagContainer::new);
    public static final Supplier<MenuType<GoldenRandomizerBagContainer>> GOLDEN_RANDOMIZER_BAG_CONTAINER = Services.PLATFORM.registerMenu("golden_randomizer_bag", GoldenRandomizerBagContainer::new);
    public static final Supplier<MenuType<DiamondRandomizerBagContainer>> DIAMOND_RANDOMIZER_BAG_CONTAINER = Services.PLATFORM.registerMenu("diamond_randomizer_bag", DiamondRandomizerBagContainer::new);
    public static final Supplier<MenuType<OmegaRandomizerBagContainer>> OMEGA_RANDOMIZER_BAG_CONTAINER = Services.PLATFORM.registerMenu("omega_randomizer_bag", OmegaRandomizerBagContainer::new);

    private static boolean backpacksUpgradeContainersRegistered;

    private SophisticatedBuilding() {
    }

    /**
     * Initialises this class, which registers the mod's content through the platform services. Called
     * first by every loader entry point (NeoForge registers its deferred registers afterwards).
     */
    public static void init() {
        logger.debug("Registered Sophisticated Building content on {}", Services.PLATFORM.getPlatformName());
    }

    /**
     * Registers the Sophisticated Backpacks upgrade container types of the Building Upgrade items, once.
     * Called by the loader projects when the registries are complete.
     */
    public static void registerBackpacksUpgradeContainers() {
        if (backpacksUpgradeContainersRegistered || !CompatHelper.isSophisticatedBackpacksLoaded()) {
            return;
        }

        try {
            Services.backpacks().registerUpgradeContainers(
                    BUILDING_UPGRADE_1.get(),
                    BUILDING_UPGRADE_2.get(),
                    BUILDING_UPGRADE_3.get(),
                    BUILDING_UPGRADE_4.get(),
                    BUILDING_UPGRADE_OMEGA.get());
            backpacksUpgradeContainersRegistered = true;
            logger.info("Registered Sophisticated Backpacks upgrade containers");
        } catch (Exception | LinkageError e) {
            logger.warn("Failed to register building upgrade containers: {}", e.toString());
        }
    }

    /**
     * Creates the real upgrade item when SophisticatedBackpacks is loaded; otherwise returns a placeholder.
     */
    private static Item createBuildingUpgrade(Item.Properties properties, int tier, int maxBlocks) {
        if (CompatHelper.isSophisticatedBackpacksLoaded()) {
            try {
                Item upgrade = Services.backpacks().createBuildingUpgrade(properties, tier, maxBlocks);
                if (upgrade != null) {
                    return upgrade;
                }
            } catch (Exception | LinkageError e) {
                logger.warn("Failed to create BuildingUpgradeItem, SophisticatedBackpacks may not be loaded properly: {}", e.toString());
            }
        }
        return new Item(properties.stacksTo(1));
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

    /** A (translatable) message to the player; a translatable one sent from the server is translated on the client. */
    public static void log(Player player, Component msg) {
        log(player, msg, false);
    }

    public static void log(Player player, Component msg, boolean actionBar) {
        player.displayClientMessage(msg, actionBar);
    }

    // Log with translation supported, call either on client or server (which then sends a message)
    public static void logTranslate(Player player, String prefix, String translationKey, String suffix, boolean actionBar) {
        if (Services.PLATFORM.isPhysicalClient()) {
            ClientProxy.logTranslate(player, prefix, translationKey, suffix, actionBar);
        } else {
            ServerProxy.logTranslate(player, prefix, translationKey, suffix, actionBar);
        }
    }

    //A red message to the player (chat or action bar), translated by the client (the mod is on both sides)
    public static void message(Player player, boolean actionBar, String translationKey, Object... args) {
        player.displayClientMessage(Component.translatable(translationKey, args).withStyle(ChatFormatting.RED), actionBar);
    }

    public static void logError(String msg) {
        logger.error(msg);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
