package sophisticated.building.forge;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
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
 * Client registrations on the mod event bus, and the HUD for Minecraft 1.21 (Forge 51).
 * <p>
 * Forge 51 has no way to add HUD layers: {@code AddGuiOverlayLayersEvent}/{@code ForgeLayeredDraw} are Forge 52+, its
 * {@code RegisterGuiOverlaysEvent} is never posted and {@code RenderGuiEvent} only comes from the unused
 * {@code ForgeGui}. {@link sophisticated.building.forge.mixin.GuiMixin} calls {@link #renderHud} after vanilla's HUD.
 */
public class SophisticatedBuildingForgeClient {

    private static final MaterialCostOverlay MATERIAL_COST_OVERLAY = new MaterialCostOverlay();

    public static void onConstructorClient(IEventBus modEventBus) {
        modEventBus.addListener(SophisticatedBuildingForgeClient::onClientSetup);
        modEventBus.addListener(SophisticatedBuildingForgeClient::registerKeyMappings);
    }

    /**
     * The HUD layers the 1.21.1 build adds through {@code AddGuiOverlayLayersEvent}, drawn after vanilla's whole HUD:
     * the material cost overlay (there above the crosshair, hidden with the HUD) and the build hints, stacks and HUDs
     * (there on top of the whole HUD, like NeoForge's RenderGuiEvent.Post).
     */
    public static void renderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!Minecraft.getInstance().options.hideGui) {
            MATERIAL_COST_OVERLAY.render(guiGraphics, deltaTracker);
        }
        RenderHandler.onRenderGui(guiGraphics);
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
