package sophisticated.building.forge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.gui.GuiGraphics;
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
public class SophisticatedBuildingForgeClient {

    public static void onConstructorClient(IEventBus modEventBus) {
        modEventBus.addListener(SophisticatedBuildingForgeClient::onClientSetup);
    }

    /**
     * Forge 1.16.5 has no HUD overlay registry: the material cost overlay is drawn right after the crosshair, where the
     * newer Forge versions register it (above the crosshair element).
     */
    private static void registerGuiOverlays() {
        MaterialCostOverlay overlay = new MaterialCostOverlay();
        MinecraftForge.EVENT_BUS.addListener((RenderGameOverlayEvent.Post event) -> {
            if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
                overlay.render(new GuiGraphics(event.getMatrixStack()), event.getPartialTicks());
            }
        });
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        registerKeyMappings();
        registerGuiOverlays();
        event.enqueueWork(() -> {
            MenuScreens.register(SophisticatedBuilding.RANDOMIZER_BAG_CONTAINER.get(), RandomizerBagScreen::new);
            MenuScreens.register(SophisticatedBuilding.GOLDEN_RANDOMIZER_BAG_CONTAINER.get(), GoldenRandomizerBagScreen::new);
            MenuScreens.register(SophisticatedBuilding.DIAMOND_RANDOMIZER_BAG_CONTAINER.get(), DiamondRandomizerBagScreen::new);
            MenuScreens.register(SophisticatedBuilding.OMEGA_RANDOMIZER_BAG_CONTAINER.get(), OmegaRandomizerBagScreen::new);
        });
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

    private static void registerKeyMappings() {
        // Register keybindings for mod controls
        for (KeyMapping keyBinding : ClientEvents.keyBindings) {
            ClientRegistry.registerKeyBinding(keyBinding);
        }
    }

    /** The local player, the context player of clientbound payloads (kept here so a server never loads client classes). */
    public static Player localPlayer() {
        return Minecraft.getInstance().player;
    }
}
