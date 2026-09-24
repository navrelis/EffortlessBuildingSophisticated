package sophisticated.building.fabric;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.gui.MaterialCostOverlay;
import sophisticated.building.config.ModConfigs;
import sophisticated.building.gui.DiamondRandomizerBagScreen;
import sophisticated.building.gui.GoldenRandomizerBagScreen;
import sophisticated.building.gui.OmegaRandomizerBagScreen;
import sophisticated.building.gui.RandomizerBagScreen;
import sophisticated.building.render.RenderHandler;

public final class FabricClientEvents {
    private static Screen lastScreen;
    private static ClientLevel lastWorld;
    private static final MaterialCostOverlay MATERIAL_COST_OVERLAY = new MaterialCostOverlay();

    private FabricClientEvents() {
    }

    public static void register() {
        registerKeyMappings();
        registerMenuScreens();
        registerLifecycleEvents();
        registerRenderEvents();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientEvents.onLoggingOut();
            // Drop the values synced from the server we just left.
            ModConfigs.restoreLocalServer();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(ClientEvents::onLoggingIn));
    }

    private static void registerKeyMappings() {
        for (var keyBinding : ClientEvents.keyBindings) {
            KeyMappingHelper.registerKeyMapping(keyBinding);
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
            sophisticated.building.create.events.ClientEvents.onTick();

            // Fabric has no screen opening event: detect the change once per tick.
            Screen currentScreen = client.screen;
            if (currentScreen != lastScreen) {
                if (currentScreen != null) {
                    ClientEvents.onGuiOpen(currentScreen);
                }
                lastScreen = currentScreen;
            }
        });

        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> {
            if (lastWorld != null && lastWorld != world) {
                sophisticated.building.create.events.ClientEvents.onUnloadWorld(lastWorld);
            }
            if (world != null) {
                sophisticated.building.create.events.ClientEvents.onLoadWorld(world);
            }
            lastWorld = world;
        });
    }

    private static void registerRenderEvents() {
        // Block previews, mirror/array lines, ghost blocks and outlines all after the translucent
        // blocks, where Catnip drew its outliner on Fabric (Fabric API for 1.21.9+: the end of the main pass, which
        // follows the translucent terrain; AFTER_TRANSLUCENT is gone).
        LevelRenderEvents.END_MAIN.register(context -> {
            if (context.poseStack() == null) {
                return;
            }
            RenderHandler.onRenderWorld(context.poseStack());
            RenderHandler.onRenderOutlines(context.poseStack());
        });

        // Last HUD element, where the deprecated HudRenderCallback drew (Fabric API for 1.21.6+).
        HudElementRegistry.addLast(SophisticatedBuilding.asResource("hud"), (guiGraphics, deltaTracker) -> {
            RenderHandler.onRenderGui(guiGraphics);
            MATERIAL_COST_OVERLAY.extractRenderState(guiGraphics, deltaTracker);
        });
    }
}
