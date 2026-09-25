package sophisticated.building;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.client.ClientBackpackItemCache;
import sophisticated.building.client.ClientBackpackToolCache;
import sophisticated.building.client.ClientBreakCountdown;
import sophisticated.building.client.ClientBuildingUpgradeState;
import sophisticated.building.gui.buildmode.PlayerSettingsGui;
import sophisticated.building.gui.buildmode.RadialMenu;
import sophisticated.building.gui.buildmodifier.ModifiersScreen;
import sophisticated.building.platform.ClientServices;
import org.lwjgl.glfw.GLFW;

/**
 * Client event logic shared by the loaders. The loader projects register {@link #keyBindings} and
 * call these methods from their client tick, key input, screen and connection events.
 */
public class ClientEvents {

    public static KeyMapping[] keyBindings;
    /** Index of the "Open player settings" key in {@link #keyBindings} (unbound by default). */
    public static final int PLAYER_SETTINGS_KEY = 6;
    public static int ticksInGame = 0;
    private static int placeCooldown = 0;
    private static int breakCooldown = 0;

    // Static initializer to set up keybindings
    static {
        keyBindings = new KeyMapping[7];
        keyBindings[0] = ClientServices.CLIENT.createKeyMapping("key.sophisticatedbuilding.mode.desc", GLFW.GLFW_KEY_LEFT_ALT, false, "key.sophisticatedbuilding.category");
        keyBindings[1] = ClientServices.CLIENT.createKeyMapping("key.sophisticatedbuilding.hud.desc", GLFW.GLFW_KEY_KP_ADD, false, "key.sophisticatedbuilding.category");
        keyBindings[2] = ClientServices.CLIENT.createKeyMapping("key.sophisticatedbuilding.undo.desc", GLFW.GLFW_KEY_Z, true, "key.sophisticatedbuilding.category");
        keyBindings[3] = ClientServices.CLIENT.createKeyMapping("key.sophisticatedbuilding.redo.desc", GLFW.GLFW_KEY_Y, true, "key.sophisticatedbuilding.category");
        keyBindings[4] = ClientServices.CLIENT.createKeyMapping("key.sophisticatedbuilding.previous_build_mode.desc", InputConstants.UNKNOWN.getValue(), false, "key.sophisticatedbuilding.category");
        keyBindings[5] = ClientServices.CLIENT.createKeyMapping("key.sophisticatedbuilding.disable_build_mode_toggle.desc", InputConstants.UNKNOWN.getValue(), false, "key.sophisticatedbuilding.category");
        keyBindings[PLAYER_SETTINGS_KEY] = ClientServices.CLIENT.createKeyMapping("key.sophisticatedbuilding.player_settings.desc", InputConstants.UNKNOWN.getValue(), false, "key.sophisticatedbuilding.category");
    }

    public static void onClientTickPre() {
        if (!isGameActive()) return;

        SophisticatedBuildingClient.BUILDER_CHAIN.onTick();

        onMouseInput();

        SophisticatedBuildingClient.BLOCK_PREVIEWS.onTick();

        ClientBreakCountdown.tick();
    }

    public static void onClientTickPost() {
        if (!isGameActive()) return;

        Screen gui = Minecraft.getInstance().screen;
        if (gui == null || !gui.isPauseScreen()) {
            ticksInGame++;
        }
    }

    private static void onMouseInput() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        BuildModeEnum buildMode = SophisticatedBuildingClient.BUILD_MODES.getBuildMode();

        if (mc.screen != null ||
            RadialMenu.instance.isVisible()) {
            return;
        }

        if (mc.options.keyUse.isDown()) {

            if (placeCooldown <= 0) {
                placeCooldown = 4;

                SophisticatedBuildingClient.BUILDER_CHAIN.onRightClick();

            } else if (buildMode == BuildModeEnum.SINGLE) {
                placeCooldown--;
                if (ModeOptions.getBuildSpeed() == ModeOptions.ActionEnum.FAST_SPEED) placeCooldown = 0;
            }
        } else {
            placeCooldown = 0;
        }

        if (mc.options.keyAttack.isDown()) {

            //Break block in distance in creative (or survival if enabled in config)
            if (breakCooldown <= 0) {
                breakCooldown = 4;

                SophisticatedBuildingClient.BUILDER_CHAIN.onLeftClick();

            } else if (buildMode == BuildModeEnum.SINGLE) {
                breakCooldown--;
                if (ModeOptions.getBuildSpeed() == ModeOptions.ActionEnum.FAST_SPEED) breakCooldown = 0;
            }

        } else {
            breakCooldown = 0;
        }
    }

    public static void onKeyPress() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        //Radial menu
        if (keyBindings[0].isDown()) {
            if (!AttachmentHandler.isDisabled(player)) {
                if (!RadialMenu.instance.isVisible()) {
                    Minecraft.getInstance().setScreen(RadialMenu.instance);
                }
            } else {
                SophisticatedBuilding.log(player, Component.translatable("sophisticatedbuilding.message.build_modes_disabled"));
            }
        }

        //Show Modifier Settings GUI
        if (keyBindings[1].consumeClick()) {
            openModifierSettings();
        }

        //Undo (Ctrl+Z)
        if (keyBindings[2].consumeClick() && ClientServices.CLIENT.isControlModifierSatisfied()) {
            ModeOptions.performAction(player, ModeOptions.ActionEnum.UNDO);
        }

        //Redo (Ctrl+Y)
        if (keyBindings[3].consumeClick() && ClientServices.CLIENT.isControlModifierSatisfied()) {
            ModeOptions.performAction(player, ModeOptions.ActionEnum.REDO);
        }

        //Previous build mode
        if (keyBindings[4].consumeClick()) {
            ModeOptions.performAction(player, ModeOptions.ActionEnum.PREVIOUS_BUILD_MODE);
        }

        //Disable build mode toggle
        if (keyBindings[5].consumeClick()) {
            ModeOptions.performAction(player, ModeOptions.ActionEnum.DISABLE_BUILD_MODE_TOGGLE);
        }

        //Player settings
        if (keyBindings[PLAYER_SETTINGS_KEY].consumeClick()) {
            ModeOptions.performAction(player, ModeOptions.ActionEnum.OPEN_PLAYER_SETTINGS);
        }
    }

    public static void openModifierSettings() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        //Disabled if max reach is 0, might be set in the config that way.
        if (AttachmentHandler.isDisabled(player)) {
            SophisticatedBuilding.log(player, Component.translatable("sophisticatedbuilding.message.build_modifiers_disabled"));
        } else {
            mc.setScreen(new ModifiersScreen());
        }
    }

    public static void openPlayerSettings() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new PlayerSettingsGui());
    }

    public static void onGuiOpen(Screen newScreen) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // Opening the radial menu must not cancel an in-progress build (F2): the player may be
        // switching an option (fill, thickness, ...) mid-build, or undoing/redoing.
        // BuildModes.setBuildMode cancels the chain when the build mode itself changes.
        if (newScreen instanceof RadialMenu) {
            return;
        }

        SophisticatedBuildingClient.BUILDER_CHAIN.cancel();
    }

    public static void onLoggingOut() {
        ClientBackpackItemCache.clear();
        ClientBuildingUpgradeState.clear();
        ClientBackpackToolCache.clear();
        ClientBreakCountdown.clear();
    }

    public static void onLoggingIn() {
        try {
            SophisticatedBuildingClient.BUILD_MODES.resyncToServer();
        } catch (Exception e) {
            SophisticatedBuilding.logger.warn("Failed to resync build mode state to the server: {}", e.getMessage());
        }
    }

    public static boolean isKeybindDown(int keybindIndex) {
        KeyMapping keyMapping = keyBindings[keybindIndex];
        if (keyMapping.isDown()) {
            return true;
        }

        // KeyMapping#isDown can briefly desync while a screen is open (e.g. ALT, since opening a
        // screen on Fabric warps the OS cursor). Fall back to the actually bound key/button rather
        // than a hard-coded GLFW_KEY_LEFT_ALT, so a rebound radial key still works.
        InputConstants.Key boundKey = ClientServices.CLIENT.getBoundKey(keyMapping);
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (boundKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, boundKey.getValue()) == GLFW.GLFW_PRESS;
        }
        if (boundKey.getType() == InputConstants.Type.KEYSYM || boundKey.getType() == InputConstants.Type.SCANCODE) {
            return InputConstants.isKeyDown(window, boundKey.getValue());
        }

        return false;
    }

    public static boolean isGameActive() {
        return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
    }

}

