package sophisticated.building.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.gui.MaterialCostOverlay;
import sophisticated.building.gui.DiamondRandomizerBagScreen;
import sophisticated.building.gui.GoldenRandomizerBagScreen;
import sophisticated.building.gui.OmegaRandomizerBagScreen;
import sophisticated.building.gui.RandomizerBagScreen;
import sophisticated.building.render.RenderHandler;

/**
 * Client registrations on the mod event bus.
 */
public class SophisticatedBuildingForgeClient {

    public static void onConstructorClient(BusGroup modBusGroup) {
        FMLClientSetupEvent.getBus(modBusGroup).addListener(SophisticatedBuildingForgeClient::onClientSetup);
        RegisterKeyMappingsEvent.BUS.addListener(SophisticatedBuildingForgeClient::registerKeyMappings);
        AddGuiOverlayLayersEvent.BUS.addListener(SophisticatedBuildingForgeClient::addGuiOverlayLayers);
    }

    public static void addGuiOverlayLayers(AddGuiOverlayLayersEvent event) {
        ForgeLayeredDraw root = event.getLayeredDraw();
        root.addAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, SophisticatedBuilding.asResource("material_cost_overlay"), ForgeLayeredDraw.CROSSHAIR, new MaterialCostOverlay()::render);
        // Build hints, stacks and HUDs on top of the whole HUD, like NeoForge's RenderGuiEvent.Post.
        root.add(SophisticatedBuilding.asResource("hud"), (guiGraphics, deltaTracker) -> RenderHandler.onRenderGui(guiGraphics));
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(SophisticatedBuilding.RANDOMIZER_BAG_CONTAINER.get(), RandomizerBagScreen::new);
            MenuScreens.register(SophisticatedBuilding.GOLDEN_RANDOMIZER_BAG_CONTAINER.get(), GoldenRandomizerBagScreen::new);
            MenuScreens.register(SophisticatedBuilding.DIAMOND_RANDOMIZER_BAG_CONTAINER.get(), DiamondRandomizerBagScreen::new);
            MenuScreens.register(SophisticatedBuilding.OMEGA_RANDOMIZER_BAG_CONTAINER.get(), OmegaRandomizerBagScreen::new);
        });
    }

    public static void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        // Register keybindings for mod controls
        for (var keyBinding : ClientEvents.keyBindings) {
            event.register(keyBinding);
        }
    }

    /** The local player, the context player of clientbound payloads (kept here so a server never loads client classes). */
    public static Player localPlayer() {
        return Minecraft.getInstance().player;
    }
}
