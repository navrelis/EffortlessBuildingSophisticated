package sophisticated.building;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.network.chat.Component;
import sophisticated.building.buildmode.BuildModes;
import sophisticated.building.client.gui.MaterialCostOverlay;
import sophisticated.building.buildmodifier.BuildModifiers;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.gui.DiamondRandomizerBagScreen;
import sophisticated.building.gui.GoldenRandomizerBagScreen;
import sophisticated.building.gui.OmegaRandomizerBagScreen;
import sophisticated.building.gui.RandomizerBagScreen;
import sophisticated.building.render.BlockPreviews;
import sophisticated.building.systems.BuildSettings;
import sophisticated.building.systems.BuilderChain;
import sophisticated.building.systems.BuilderFilter;
import sophisticated.building.systems.ItemUsageTracker;

public class SophisticatedBuildingClient {

    public static final BuilderChain BUILDER_CHAIN = new BuilderChain();
    public static final BuildModes BUILD_MODES = new BuildModes();
    public static final BuildModifiers BUILD_MODIFIERS = new BuildModifiers();
    public static final BuildSettings BUILD_SETTINGS = new BuildSettings();
    public static final BlockPreviews BLOCK_PREVIEWS = new BlockPreviews();
    public static final BuilderFilter BUILDER_FILTER = new BuilderFilter();
    public static final ItemUsageTracker ITEM_USAGE_TRACKER = new ItemUsageTracker();

    public static void onConstructorClient(IEventBus modEventBus, IEventBus forgeEventBus) {
        modEventBus.addListener(SophisticatedBuildingClient::registerMenuScreens);
        modEventBus.addListener(SophisticatedBuildingClient::registerKeyMappings);
        modEventBus.addListener(SophisticatedBuildingClient::onClientSetup);
        modEventBus.addListener(SophisticatedBuildingClient::registerGuiLayers);
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, SophisticatedBuilding.asResource("material_cost_overlay"), new MaterialCostOverlay());
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        // Only register upgrade GUI tab if SophisticatedBackpacks is loaded
        if (CompatHelper.isSophisticatedBackpacksLoaded()) {
            event.enqueueWork(() -> {
                try {
                    sophisticated.building.integration.SophisticatedBackpacksClientIntegration.registerUpgradeTab();
                } catch (NoClassDefFoundError | Exception e) {
                    SophisticatedBuilding.logger.warn("Failed to register building upgrade tab: {}", e.getMessage());
                }
            });
        }
    }

    public static void registerMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(SophisticatedBuilding.RANDOMIZER_BAG_CONTAINER.get(), RandomizerBagScreen::new);
        event.register(SophisticatedBuilding.GOLDEN_RANDOMIZER_BAG_CONTAINER.get(), GoldenRandomizerBagScreen::new);
        event.register(SophisticatedBuilding.DIAMOND_RANDOMIZER_BAG_CONTAINER.get(), DiamondRandomizerBagScreen::new);
        event.register(SophisticatedBuilding.OMEGA_RANDOMIZER_BAG_CONTAINER.get(), OmegaRandomizerBagScreen::new);
    }

    public static void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        // Register keybindings for mod controls
        for (var keyBinding : ClientEvents.keyBindings) {
            event.register(keyBinding);
        }
    }
}
