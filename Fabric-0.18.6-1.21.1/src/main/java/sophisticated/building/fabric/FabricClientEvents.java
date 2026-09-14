package sophisticated.building.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.client.ClientBackpackItemCache;
import sophisticated.building.client.ClientBackpackToolCache;
import sophisticated.building.client.ClientBreakCountdown;
import sophisticated.building.client.ClientBuildingUpgradeState;
import sophisticated.building.client.gui.MaterialCostOverlay;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.gui.DiamondRandomizerBagScreen;
import sophisticated.building.gui.GoldenRandomizerBagScreen;
import sophisticated.building.gui.OmegaRandomizerBagScreen;
import sophisticated.building.gui.RandomizerBagScreen;
import sophisticated.building.render.RenderHandler;

public final class FabricClientEvents {
    private static Screen lastScreen;
    private static ClientLevel lastWorld;
    private static final MaterialCostOverlay MATERIAL_COST_OVERLAY = new MaterialCostOverlay();
    private static boolean optionalIntegrationsRegistered;
    private static boolean catnipUnavailableLogged;

    private FabricClientEvents() {
    }

    public static void register() {
        registerKeyMappings();
        registerMenuScreens();
        registerLifecycleEvents();
        registerRenderEvents();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientBackpackItemCache.clear();
            ClientBuildingUpgradeState.clear();
            ClientBackpackToolCache.clear();
            ClientBreakCountdown.clear();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> {
            try {
                SophisticatedBuildingClient.BUILD_MODES.resyncToServer();
            } catch (Exception e) {
                SophisticatedBuilding.logger.warn("Failed to resync build mode state to the server: {}", e.getMessage());
            }
        }));
    }

    private static void registerKeyMappings() {
        for (var keyBinding : ClientEvents.keyBindings) {
            KeyBindingHelper.registerKeyBinding(keyBinding);
        }
    }

    private static void registerMenuScreens() {
        MenuScreens.register(SophisticatedBuilding.RANDOMIZER_BAG_CONTAINER.get(), RandomizerBagScreen::new);
        MenuScreens.register(SophisticatedBuilding.GOLDEN_RANDOMIZER_BAG_CONTAINER.get(), GoldenRandomizerBagScreen::new);
        MenuScreens.register(SophisticatedBuilding.DIAMOND_RANDOMIZER_BAG_CONTAINER.get(), DiamondRandomizerBagScreen::new);
        MenuScreens.register(SophisticatedBuilding.OMEGA_RANDOMIZER_BAG_CONTAINER.get(), OmegaRandomizerBagScreen::new);
    }

    private static void registerLifecycleEvents() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientEvents.onClientTickPre());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientEvents.onClientTickPost();
            ClientEvents.onKeyPress();
            if (CompatHelper.isCatnipLoaded()) {
                sophisticated.building.create.events.ClientEvents.onTick();
            }

            Screen currentScreen = client.screen;
            if (currentScreen != lastScreen) {
                // Opening the radial menu must not cancel an in-progress build (F2): the player may
                // be switching an option (fill, thickness, ...) mid-build, or undoing/redoing.
                // BuildModes.setBuildMode cancels the chain when the build mode itself changes.
                if (currentScreen != null && !(currentScreen instanceof sophisticated.building.gui.buildmode.RadialMenu)) {
                    ClientEvents.onGuiOpen();
                }
                lastScreen = currentScreen;
            }
        });

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            if (CompatHelper.isCatnipLoaded()) {
                if (lastWorld != null && lastWorld != world) {
                    sophisticated.building.create.events.ClientEvents.onUnloadWorld(lastWorld);
                    sophisticated.building.create.events.CommonEvents.onUnloadWorld(lastWorld);
                }
                if (world != null) {
                    sophisticated.building.create.events.ClientEvents.onLoadWorld(world);
                }
            }
            lastWorld = world;
        });
    }

    private static void registerRenderEvents() {
        if (CompatHelper.isCatnipLoaded()) {
            WorldRenderEvents.AFTER_TRANSLUCENT.register(RenderHandler::onRenderWorld);
        } else {
            logCatnipUnavailableOnce();
        }

        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            if (CompatHelper.isCatnipLoaded()) {
                RenderHandler.onRenderGui(guiGraphics);
            }
            MATERIAL_COST_OVERLAY.render(guiGraphics, deltaTracker);
        });
    }

    private static void logCatnipUnavailableOnce() {
        if (!catnipUnavailableLogged) {
            SophisticatedBuilding.logger.info("Catnip/Create rendering hooks are unavailable, skipping optional client render integrations.");
            catnipUnavailableLogged = true;
        }
    }

    public static void registerOptionalIntegrations() {
        if (optionalIntegrationsRegistered) {
            return;
        }

        if (CompatHelper.isSophisticatedBackpacksLoaded()) {
            try {
                sophisticated.building.integration.SophisticatedBackpacksClientIntegration.registerUpgradeTab();
                optionalIntegrationsRegistered = true;
            } catch (NoClassDefFoundError | Exception e) {
                SophisticatedBuilding.logger.warn("Failed to register building upgrade tab: {}", e.getMessage());
            }
        } else {
            optionalIntegrationsRegistered = true;
        }
    }
}
