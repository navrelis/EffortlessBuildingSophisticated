package sophisticated.building.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.gui.GuiGraphics;
import sophisticated.building.client.gui.MaterialCostOverlay;
import sophisticated.building.gui.DiamondRandomizerBagScreen;
import sophisticated.building.gui.GoldenRandomizerBagScreen;
import sophisticated.building.gui.OmegaRandomizerBagScreen;
import sophisticated.building.gui.RandomizerBagScreen;

/**
 * Client registrations on the mod event bus.
 */
public class SophisticatedBuildingForgeClient {

    public static void onConstructorClient(IEventBus modEventBus) {
        modEventBus.addListener(SophisticatedBuildingForgeClient::onClientSetup);
        modEventBus.addListener(SophisticatedBuildingForgeClient::registerKeyMappings);
        modEventBus.addListener(SophisticatedBuildingForgeClient::registerGuiOverlays);
    }

    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        MaterialCostOverlay overlay = new MaterialCostOverlay();
        event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "material_cost_overlay",
                (gui, poseStack, partialTick, screenWidth, screenHeight) -> overlay.render(new GuiGraphics(poseStack), partialTick));
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
