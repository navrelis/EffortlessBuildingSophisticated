package sophisticated.building.smoketest.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.Platform;
import org.lwjgl.system.windows.User32;
import sophisticated.building.smoketest.SmokeTest;

import java.lang.reflect.Field;

/**
 * Keeps a smoke client out of the way and undisturbed: muted (master volume 0, in memory only), moved to the first
 * monitor that is not the primary one (left in place with a single monitor), deaf to real keyboard and mouse input
 * (its GLFW input callbacks are removed), so clicking into the window cannot move the player or change a selection
 * mid-run, and never touching the user's OS cursor (see {@link #keepOffTheCursor}). The harness drives the game
 * without any of them. Must run on the client (main) thread.
 */
final class ClientWindow {

    private ClientWindow() {
    }

    private static void detachInput(long window) {
        if (window == 0) return;
        GLFW.glfwSetKeyCallback(window, null);
        GLFW.glfwSetCharModsCallback(window, null);
        GLFW.glfwSetMouseButtonCallback(window, null);
        GLFW.glfwSetCursorPosCallback(window, null);
        GLFW.glfwSetScrollCallback(window, null);
        GLFW.glfwSetDropCallback(window, null);
        GLFW.glfwSetCursorEnterCallback(window, null);
        // Focus changes must not reach the game: an active window grabs the mouse (below)
        GLFW.glfwSetWindowFocusCallback(window, null);
    }

    /**
     * The game grabs the mouse ({@code MouseHandler#grabMouse}: hidden, captured cursor warped to the window centre
     * through {@code InputConstants.grabOrReleaseMouse} -> {@code glfwSetInputMode(GLFW_CURSOR_DISABLED)} +
     * {@code glfwSetCursorPos}) whenever no screen is open and the window is active, e.g. on joining the world and after
     * every screen closes, and releases it (warping it again) when a screen opens. {@code grabMouse} does nothing while
     * {@code Minecraft#isWindowActive()} is false, and {@code releaseMouse} nothing while the mouse is not grabbed. So:
     * the window is marked inactive for good (its focus callback is gone, see {@link #detachInput}), a grab that already
     * happened is undone without moving the cursor, and the window can no longer be activated (Windows
     * {@code WS_EX_NOACTIVATE}, GLFW {@code FOCUS_ON_SHOW} off), so neither a click into it nor showing it takes the
     * focus from the user. The harness moves the game's own pointer ({@code MouseHandler#xpos/ypos}) and calls the
     * input handlers directly, which never touches the OS cursor.
     */
    static String keepOffTheCursor(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        setField(Minecraft.class, mc, "windowActive", false);
        if (mc.mouseHandler.isMouseGrabbed()) {
            setField(MouseHandler.class, mc.mouseHandler, "mouseGrabbed", false);
            if (window != 0) GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
        if (window == 0) return "window inactive";
        GLFW.glfwSetWindowAttrib(window, GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        if (Platform.get() == Platform.WINDOWS) {
            long hwnd = GLFWNativeWin32.glfwGetWin32Window(window);
            if (hwnd != 0) {
                long style = User32.GetWindowLongPtr(hwnd, User32.GWL_EXSTYLE);
                User32.SetWindowLongPtr(hwnd, User32.GWL_EXSTYLE, style | User32.WS_EX_NOACTIVATE);
                return "window inactive, cannot be activated (WS_EX_NOACTIVATE), mouse never grabbed";
            }
        }
        return "window inactive, mouse never grabbed";
    }

    private static void setField(Class<?> owner, Object target, String name, boolean value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.setBoolean(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(owner.getSimpleName() + "#" + name + " (the harness keeps the OS cursor free through it)", e);
        }
    }

    static String muteAndMoveAside(Minecraft mc) {
        mc.options.setSoundCategoryVolume(SoundSource.MASTER, 0.0F);

        long window = mc.getWindow().getWindow();
        detachInput(window);
        String cursor = keepOffTheCursor(mc);
        long primary = GLFW.glfwGetPrimaryMonitor();
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (window == 0 || primary == 0 || monitors == null) return "muted, input detached, " + cursor + "; no monitor information";

        int[] primaryX = new int[1];
        int[] primaryY = new int[1];
        GLFW.glfwGetMonitorPos(primary, primaryX, primaryY);
        for (int i = 0; i < monitors.limit(); i++) {
            long monitor = monitors.get(i);
            if (monitor == primary) continue;
            int[] x = new int[1];
            int[] y = new int[1];
            // The monitor's position: LWJGL 3.2.1 of Minecraft 1.18.2 has no glfwGetMonitorWorkarea
            GLFW.glfwGetMonitorPos(monitor, x, y);
            if (x[0] == primaryX[0] && y[0] == primaryY[0]) continue;
            GLFW.glfwSetWindowPos(window, x[0] + 40, y[0] + 40);
            String result = "muted, input detached, " + cursor + "; window moved to monitor " + GLFW.glfwGetMonitorName(monitor)
                    + " at " + (x[0] + 40) + "," + (y[0] + 40);
            SmokeTest.LOGGER.info("Smoke client {}", result);
            return result;
        }
        String result = "muted, input detached, " + cursor + "; single monitor, window left in place";
        SmokeTest.LOGGER.info("Smoke client {}", result);
        return result;
    }
}
