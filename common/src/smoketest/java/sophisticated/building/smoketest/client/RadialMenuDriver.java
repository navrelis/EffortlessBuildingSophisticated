package sophisticated.building.smoketest.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import sophisticated.building.ClientEvents;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.buildmode.BuildModeEnum;
import sophisticated.building.buildmode.ModeOptions;
import sophisticated.building.gui.buildmode.RadialMenu;
import sophisticated.building.platform.ClientServices;

import java.lang.reflect.Field;

/**
 * Selects a build mode the way a player does: hold the radial menu key (the mod's key handler opens
 * {@link RadialMenu}), move the mouse over the mode's ring sector (the menu's own render pass hit-tests it and
 * highlights the mode), left-click, release the key (the menu closes on its next tick).
 * <p>
 * The only shortcut is the mouse movement: {@link RadialMenu} tracks the mouse as an offset from the screen center
 * that it accumulates from raw cursor deltas, so the harness writes that offset instead of warping the OS cursor.
 */
final class RadialMenuDriver {

    private final ClientDriver d;
    private Runnable restoreKey = () -> {
    };

    RadialMenuDriver(ClientDriver driver) {
        this.d = driver;
    }

    /** Opens the menu and keeps it open (the radial key stays held). Returns once it rendered a few frames. */
    void open() {
        KeyMapping radialKey = ClientEvents.keyBindings[0];
        InputConstants.Key key = d.client(() -> ClientServices.CLIENT.getBoundKey(radialKey));
        restoreKey = d.client(() -> SmokeClientPlatform.get().keepHeldInScreens(radialKey));
        d.clientRun(() -> {
            KeyMapping.set(key, true);
            // What the loaders call on key input (Fabric also every tick)
            ClientEvents.onKeyPress();
            // Opening a screen releases every key mapping; the player still holds the key
            KeyMapping.set(key, true);
        });
        d.waitUntil("the radial menu to open", 40, () -> d.mc.screen instanceof RadialMenu);
        d.waitTicks(5);
        d.waitUntil("the radial menu to render (mouse tracking initialised)", 40, () -> getBoolean("mouseInitialized"));
    }

    /** Moves the mouse onto the mode's sector and waits until the menu's render pass highlights it. */
    void hover(BuildModeEnum mode) {
        d.clientRun(() -> {
            int total = Math.max(3, BuildModeEnum.values().length);
            double radiansPerMode = 2.0 * Math.PI / total;
            double angle = (mode.ordinal() + 0.5) * radiansPerMode - Math.PI / 2.0;
            double radius = (getDouble("ringInnerEdge") + getDouble("ringOuterEdge")) / 2.0;
            set("accumulatedMouseX", Math.cos(angle) * radius);
            set("accumulatedMouseY", Math.sin(angle) * radius);
        });
        d.waitUntil("the radial menu to highlight " + mode, 40, () -> RadialMenu.instance.switchTo == mode);
    }

    /**
     * Moves the mouse onto a side button (action or build mode option) where the menu last drew it
     * ({@link RadialMenu#sideButtons()}), and waits until the menu's render pass highlights it.
     */
    void hoverButton(ModeOptions.ActionEnum action) {
        d.waitUntil("the radial menu to draw the " + action + " button", 40, () -> button(action) != null);
        d.clientRun(() -> {
            RadialMenu.SideButton button = button(action);
            set("accumulatedMouseX", (button.left() + button.right()) / 2 - RadialMenu.instance.width / 2.0);
            set("accumulatedMouseY", (button.top() + button.bottom()) / 2 - RadialMenu.instance.height / 2.0);
        });
        pointerToMenuMouse();
        d.waitUntil("the radial menu to highlight the " + action + " button", 40, () -> RadialMenu.instance.doAction == action);
    }

    private static RadialMenu.SideButton button(ModeOptions.ActionEnum action) {
        for (RadialMenu.SideButton button : RadialMenu.instance.sideButtons()) {
            if (button.action() == action) return button;
        }
        return null;
    }

    /** Moves the mouse to the ring's centre, where nothing is highlighted and no tooltip is drawn. */
    void hoverNothing() {
        d.clientRun(() -> {
            set("accumulatedMouseX", 0);
            set("accumulatedMouseY", 0);
        });
        pointerToMenuMouse();
        d.waitUntil("the radial menu to highlight nothing", 40, () -> RadialMenu.instance.doAction == null && RadialMenu.instance.switchTo == null);
    }

    /** Puts the real pointer where the menu's tracked mouse is, so the menu draws its tooltip next to the button. */
    private void pointerToMenuMouse() {
        double[] at = d.client(() -> new double[]{RadialMenu.instance.width / 2.0 + getDouble("accumulatedMouseX"),
                RadialMenu.instance.height / 2.0 + getDouble("accumulatedMouseY")});
        d.pointAt(at[0], at[1]);
    }

    /** Left-clicks (selects the highlighted mode) and releases the radial key, which closes the menu. */
    void clickAndRelease() {
        KeyMapping radialKey = ClientEvents.keyBindings[0];
        InputConstants.Key key = d.client(() -> ClientServices.CLIENT.getBoundKey(radialKey));
        d.clientRun(() -> {
            RadialMenu menu = RadialMenu.instance;
            double x = menu.width / 2.0 + getDouble("accumulatedMouseX");
            double y = menu.height / 2.0 + getDouble("accumulatedMouseY");
            menu.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
            KeyMapping.set(key, false);
        });
        d.clientRun(restoreKey);
        d.waitUntil("the radial menu to close after releasing its key", 40, () -> !(d.mc.screen instanceof RadialMenu));
    }

    /** Full selection; fails if the mode did not become active. */
    void select(BuildModeEnum mode) {
        open();
        hover(mode);
        clickAndRelease();
        BuildModeEnum active = d.client(SophisticatedBuildingClient.BUILD_MODES::getBuildMode);
        if (active != mode) throw new AssertionError("Selected " + mode + " in the radial menu but the active build mode is " + active);
        d.waitTicks(2);
    }

    private static Field field(String name) {
        try {
            Field field = RadialMenu.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new AssertionError("RadialMenu has no field " + name + " (the harness must follow the menu's mouse tracking)", e);
        }
    }

    private static double getDouble(String name) {
        try {
            return field(name).getDouble(RadialMenu.instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean getBoolean(String name) {
        try {
            return field(name).getBoolean(RadialMenu.instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void set(String name, double value) {
        try {
            field(name).setDouble(RadialMenu.instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
