package sophisticated.building;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.gui.buildmode.PlayerSettingsGui;
import sophisticated.building.gui.buildmode.RadialMenu;
import sophisticated.building.gui.buildmodifier.ModifiersScreen;
import org.lwjgl.glfw.GLFW;

public class ClientEvents {




    public static KeyMapping[] keyBindings;
    public static int ticksInGame = 0;
    private static int placeCooldown = 0;
    private static int breakCooldown = 0;

    // Static initializer to set up keybindings
    static {
        keyBindings = new KeyMapping[6];
        keyBindings[0] = new KeyMapping("key.sophisticatedbuilding.mode.desc", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "key.sophisticatedbuilding.category");
        keyBindings[1] = new KeyMapping("key.sophisticatedbuilding.hud.desc", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_ADD, "key.sophisticatedbuilding.category");
        keyBindings[2] = new KeyMapping("key.sophisticatedbuilding.undo.desc", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.sophisticatedbuilding.category");
        keyBindings[3] = new KeyMapping("key.sophisticatedbuilding.redo.desc", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.sophisticatedbuilding.category");
        keyBindings[4] = new KeyMapping("key.sophisticatedbuilding.previous_build_mode.desc", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.sophisticatedbuilding.category");
        keyBindings[5] = new KeyMapping("key.sophisticatedbuilding.disable_build_mode_toggle.desc", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.sophisticatedbuilding.category");
    }

    public static void onClientTickPre() {
        if (!isGameActive()) return;

        SophisticatedBuildingClient.BUILDER_CHAIN.onTick();

        onMouseInput();

        SophisticatedBuildingClient.BLOCK_PREVIEWS.onTick();
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
                SophisticatedBuilding.log(player, "Build modes are disabled until your reach has increased. Increase your reach with craftable reach upgrades.");
            }
        }

        //Show Modifier Settings GUI
        if (keyBindings[1].consumeClick()) {
            openModifierSettings();
        }

        //Undo (Ctrl+Z)
        if (keyBindings[2].consumeClick() && Screen.hasControlDown()) {
            ModeOptions.performAction(player, ModeOptions.ActionEnum.UNDO);
        }

        //Redo (Ctrl+Y)
        if (keyBindings[3].consumeClick() && Screen.hasControlDown()) {
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
    }

    public static void openModifierSettings() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        //Disabled if max reach is 0, might be set in the config that way.
        if (AttachmentHandler.isDisabled(player)) {
            SophisticatedBuilding.log(player, "Build modifiers are disabled until your power level has increased. Increase your power level by consuming certain items.");
        } else {
            mc.setScreen(new ModifiersScreen());
        }
    }

    public static void openPlayerSettings() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new PlayerSettingsGui());
    }

    public static void onGuiOpen() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            SophisticatedBuildingClient.BUILDER_CHAIN.cancel();
        }
    }

    public static boolean isKeybindDown(int keybindIndex) {
        if (keyBindings[keybindIndex].isDown()) {
            return true;
        }

        // KeyMapping#isDown can briefly desync for ALT while a screen is open.
        if (keybindIndex == 0) {
            return InputConstants.isKeyDown(
                    Minecraft.getInstance().getWindow().getWindow(),
                    GLFW.GLFW_KEY_LEFT_ALT);
        }

        return false;
    }

    public static boolean isGameActive() {
        return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
    }

}

