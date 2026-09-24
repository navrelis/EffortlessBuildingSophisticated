package sophisticated.building.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.gui.MaterialCostOverlay;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.gui.DiamondRandomizerBagScreen;
import sophisticated.building.gui.GoldenRandomizerBagScreen;
import sophisticated.building.gui.OmegaRandomizerBagScreen;
import sophisticated.building.gui.RandomizerBagScreen;
import sophisticated.building.platform.Services;

/**
 * Client registrations on the mod event bus.
 */
public class SophisticatedBuildingNeoForgeClient {

    public static void onConstructorClient(IEventBus modEventBus) {
        modEventBus.addListener(SophisticatedBuildingNeoForgeClient::registerMenuScreens);
        modEventBus.addListener(SophisticatedBuildingNeoForgeClient::registerKeyMappings);
        modEventBus.addListener(SophisticatedBuildingNeoForgeClient::onClientSetup);
        modEventBus.addListener(SophisticatedBuildingNeoForgeClient::registerGuiLayers);
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, SophisticatedBuilding.asResource("material_cost_overlay"), new MaterialCostOverlay()::render);
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        // Only register upgrade GUI tab if SophisticatedBackpacks is loaded
        if (CompatHelper.isSophisticatedBackpacksLoaded()) {
            event.enqueueWork(() -> {
                try {
                    Services.backpacks().registerUpgradeTab();
                } catch (Exception | LinkageError e) {
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
