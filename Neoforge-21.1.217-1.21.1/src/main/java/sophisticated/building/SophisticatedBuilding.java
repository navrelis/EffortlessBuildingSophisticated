package sophisticated.building;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.gui.DiamondRandomizerBagContainer;
import sophisticated.building.gui.GoldenRandomizerBagContainer;
import sophisticated.building.gui.OmegaRandomizerBagContainer;
import sophisticated.building.gui.RandomizerBagContainer;
import sophisticated.building.item.*;
import sophisticated.building.network.PacketHandler;
import sophisticated.building.proxy.ClientProxy;
import sophisticated.building.proxy.ServerProxy;
import sophisticated.building.systems.ItemUsageTracker;
import sophisticated.building.systems.ServerBlockPlacer;
import sophisticated.building.systems.UndoRedo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Mod(SophisticatedBuilding.MODID)
public class SophisticatedBuilding {

    public static final String MODID = "sophisticatedbuilding";
    public static final Logger logger = LogManager.getLogger();

    public static SophisticatedBuilding instance;

    public static final ServerBlockPlacer SERVER_BLOCK_PLACER = new ServerBlockPlacer();
    public static final UndoRedo UNDO_REDO = new UndoRedo();
    public static final ItemUsageTracker ITEM_USAGE_TRACKER = new ItemUsageTracker();
    
    // Note: BUILDING_UPGRADE_CONTAINER_TYPE is now created lazily via SophisticatedBackpacksIntegration
    // to avoid loading SophisticatedCore classes when the mod isn't present

    // Registration
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(BuiltInRegistries.MENU, MODID);
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredItem<RandomizerBagItem> RANDOMIZER_BAG_ITEM = ITEMS.register("randomizer_bag", RandomizerBagItem::new);
    public static final DeferredItem<GoldenRandomizerBagItem> GOLDEN_RANDOMIZER_BAG_ITEM = ITEMS.register("golden_randomizer_bag", GoldenRandomizerBagItem::new);
    public static final DeferredItem<DiamondRandomizerBagItem> DIAMOND_RANDOMIZER_BAG_ITEM = ITEMS.register("diamond_randomizer_bag", DiamondRandomizerBagItem::new);
    public static final DeferredItem<OmegaRandomizerBagItem> OMEGA_RANDOMIZER_BAG_ITEM = ITEMS.register("omega_randomizer_bag", OmegaRandomizerBagItem::new);
    public static final DeferredItem<ReachUpgrade1Item> REACH_UPGRADE_1_ITEM = ITEMS.register("reach_upgrade1", ReachUpgrade1Item::new);
    public static final DeferredItem<ReachUpgrade2Item> REACH_UPGRADE_2_ITEM = ITEMS.register("reach_upgrade2", ReachUpgrade2Item::new);
    public static final DeferredItem<ReachUpgrade3Item> REACH_UPGRADE_3_ITEM = ITEMS.register("reach_upgrade3", ReachUpgrade3Item::new);
    // Removed: MUSCLES_ITEM, ELASTIC_HAND_ITEM, BUILDING_TECHNIQUES_BOOK_ITEM (PowerLevelItem)

    // Compressed block items (crafting materials for building upgrades)
    public static final DeferredItem<CompressedBlockItem> COMPRESSED_DIRT = ITEMS.register("compressed_dirt", CompressedBlockItem::new);
    public static final DeferredItem<CompressedBlockItem> COMPRESSED_COBBLESTONE = ITEMS.register("compressed_cobblestone", CompressedBlockItem::new);
    public static final DeferredItem<CompressedBlockItem> COMPRESSED_SAND = ITEMS.register("compressed_sand", CompressedBlockItem::new);
    public static final DeferredItem<CompressedBlockItem> COMPRESSED_COBBLED_DEEPSLATE = ITEMS.register("compressed_cobbled_deepslate", CompressedBlockItem::new);

    // Building upgrade items (backpack upgrades for effortless building)
    // These are registered conditionally and created via the integration class to avoid loading SophisticatedCore classes when the mod isn't present
    // Tier 1: 32 blocks, Tier 2: 64 blocks, Tier 3: 128 blocks, Tier 4: 256 blocks, Omega: 2048 blocks
    public static final DeferredItem<Item> BUILDING_UPGRADE_1 = ITEMS.register("building_upgrade_1", () -> createBuildingUpgrade(1, 32));
    public static final DeferredItem<Item> BUILDING_UPGRADE_2 = ITEMS.register("building_upgrade_2", () -> createBuildingUpgrade(2, 64));
    public static final DeferredItem<Item> BUILDING_UPGRADE_3 = ITEMS.register("building_upgrade_3", () -> createBuildingUpgrade(3, 128));
    public static final DeferredItem<Item> BUILDING_UPGRADE_4 = ITEMS.register("building_upgrade_4", () -> createBuildingUpgrade(4, 256));
    public static final DeferredItem<Item> BUILDING_UPGRADE_OMEGA = ITEMS.register("building_upgrade_omega", () -> createBuildingUpgrade(5, 2048));

    // Lazy holders for building upgrades (to allow conditional adding to creative tab)
    private static final List<DeferredItem<Item>> BUILDING_UPGRADE_ITEMS = new ArrayList<>();
    static {
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_1);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_2);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_3);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_4);
        BUILDING_UPGRADE_ITEMS.add(BUILDING_UPGRADE_OMEGA);
    }

    /**
        * Creates real upgrade item when SophisticatedBackpacks is loaded; otherwise returns a placeholder.
     */
    private static Item createBuildingUpgrade(int tier, int maxBlocks) {
        if (CompatHelper.isSophisticatedBackpacksLoaded()) {
            try {
                return sophisticated.building.integration.SophisticatedBackpacksIntegration.createBuildingUpgrade(tier, maxBlocks);
            } catch (NoClassDefFoundError | Exception e) {
                logger.warn("Failed to create BuildingUpgradeItem, SophisticatedBackpacks may not be loaded properly: {}", e.getMessage());
            }
        }
        return new Item(new Item.Properties().stacksTo(1));
    }

    // Custom creative tab
    public static final Supplier<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(BUILDING_UPGRADE_OMEGA.get()))
                    .title(Component.translatable("itemGroup.sophisticatedbuilding"))
                    .displayItems((parameters, output) -> {
                        // Add all items except the creative icon itself
                        output.accept(RANDOMIZER_BAG_ITEM.get());
                        output.accept(GOLDEN_RANDOMIZER_BAG_ITEM.get());
                        output.accept(DIAMOND_RANDOMIZER_BAG_ITEM.get());
                        output.accept(OMEGA_RANDOMIZER_BAG_ITEM.get());
                        output.accept(REACH_UPGRADE_1_ITEM.get());
                        output.accept(REACH_UPGRADE_2_ITEM.get());
                        output.accept(REACH_UPGRADE_3_ITEM.get());
                        // Compressed blocks
                        output.accept(COMPRESSED_DIRT.get());
                        output.accept(COMPRESSED_COBBLESTONE.get());
                        output.accept(COMPRESSED_SAND.get());
                        output.accept(COMPRESSED_COBBLED_DEEPSLATE.get());
                        // Building upgrades (always show - they're placeholder items when SB not loaded)
                        for (DeferredItem<Item> upgradeItem : BUILDING_UPGRADE_ITEMS) {
                            output.accept(upgradeItem.get());
                        }
                    })
                    .build());

    public static final Supplier<MenuType<RandomizerBagContainer>> RANDOMIZER_BAG_CONTAINER = CONTAINERS.register("randomizer_bag", () -> registerContainer(RandomizerBagContainer::new));
    public static final Supplier<MenuType<GoldenRandomizerBagContainer>> GOLDEN_RANDOMIZER_BAG_CONTAINER = CONTAINERS.register("golden_randomizer_bag", () -> registerContainer(GoldenRandomizerBagContainer::new));
    public static final Supplier<MenuType<DiamondRandomizerBagContainer>> DIAMOND_RANDOMIZER_BAG_CONTAINER = CONTAINERS.register("diamond_randomizer_bag", () -> registerContainer(DiamondRandomizerBagContainer::new));
    public static final Supplier<MenuType<OmegaRandomizerBagContainer>> OMEGA_RANDOMIZER_BAG_CONTAINER = CONTAINERS.register("omega_randomizer_bag", () -> registerContainer(OmegaRandomizerBagContainer::new));

    public static final Supplier<MapCodec<SingleItemLootModifier>> SINGLE_ITEM_LOOT_MODIFIER = LOOT_MODIFIERS.register("single_item_loot_modifier", SingleItemLootModifier.CODEC);

    public static final Supplier<AttachmentType<PowerLevel>> POWER_LEVEL = ATTACHMENT_TYPES.register("power_level", () -> AttachmentType.serializable(PowerLevel::new).build());

    public SophisticatedBuilding(IEventBus modEventBus, ModContainer container, Dist dist) {
        instance = this;

        IEventBus forgeEventBus = NeoForge.EVENT_BUS;

        modEventBus.addListener(SophisticatedBuilding::setup);
        modEventBus.addListener(SophisticatedBuilding::registerCapabilities);
        modEventBus.addListener(PacketHandler::setupPackets);

        ITEMS.register(modEventBus);
        CONTAINERS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        LOOT_MODIFIERS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);

        // Register config
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.spec);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.spec);
        if (dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.spec);
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
            SophisticatedBuildingClient.onConstructorClient(modEventBus, forgeEventBus);
        }
    }

    public static void setup(final FMLCommonSetupEvent event) {
        CompatHelper.setup();
        if (CompatHelper.isSophisticatedBackpacksLoaded()) {
            event.enqueueWork(() -> {
                try {
                    // Register upgrade containers via integration class to avoid loading SophisticatedCore at class init time
                    sophisticated.building.integration.SophisticatedBackpacksIntegration.registerUpgradeContainers(
                        BUILDING_UPGRADE_1.get(),
                        BUILDING_UPGRADE_2.get(),
                        BUILDING_UPGRADE_3.get(),
                        BUILDING_UPGRADE_4.get(),
                        BUILDING_UPGRADE_OMEGA.get()
                    );
                } catch (NoClassDefFoundError | Exception e) {
                    logger.warn("Failed to register building upgrade containers: {}", e.getMessage());
                }
            });
        }
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Register item handler capabilities for randomizer bags
        event.registerItem(Capabilities.ItemHandler.ITEM, (stack, ctx) -> {
            if (stack.getItem() instanceof AbstractRandomizerBagItem bagItem) {
                return new ComponentItemHandler(stack, DataComponents.CONTAINER, bagItem.getInventorySize());
            }
            return null;
        }, RANDOMIZER_BAG_ITEM.get(), GOLDEN_RANDOMIZER_BAG_ITEM.get(), DIAMOND_RANDOMIZER_BAG_ITEM.get(), OMEGA_RANDOMIZER_BAG_ITEM.get());
    }

    public static <T extends AbstractContainerMenu> MenuType<T> registerContainer(net.neoforged.neoforge.network.IContainerFactory<T> fact) {
        return new MenuType<>(fact, FeatureFlags.REGISTRY.allFlags());
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

    // Log with translation supported, call either on client or server (which then sends a message)
    public static void logTranslate(Player player, String prefix, String translationKey, String suffix, boolean actionBar) {
        if (FMLEnvironment.dist.isClient()) {
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
