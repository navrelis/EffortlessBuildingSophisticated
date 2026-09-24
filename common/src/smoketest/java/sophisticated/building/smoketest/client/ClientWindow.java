package sophisticated.building.smoketest.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import sophisticated.building.smoketest.SmokeTest;

/**
 * Keeps a smoke client out of the way and undisturbed: muted (master volume 0, in memory only), moved to the first
 * monitor that is not the primary one (left in place with a single monitor), and deaf to real keyboard and mouse input
 * (its GLFW input callbacks are removed), so clicking into the window cannot move the player or change a selection
 * mid-run; the harness drives the game without them. Must run on the client (main) thread.
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
    }

    static String muteAndMoveAside(Minecraft mc) {
        mc.options.setSoundCategoryVolume(SoundSource.MASTER, 0.0F);

        long window = mc.getWindow().getWindow();
        detachInput(window);
        long primary = GLFW.glfwGetPrimaryMonitor();
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (window == 0 || primary == 0 || monitors == null) return "muted, input detached; no monitor information";

        int[] primaryX = new int[1];
        int[] primaryY = new int[1];
        GLFW.glfwGetMonitorPos(primary, primaryX, primaryY);
        for (int i = 0; i < monitors.limit(); i++) {
            long monitor = monitors.get(i);
            if (monitor == primary) continue;
            int[] x = new int[1];
            int[] y = new int[1];
            int[] width = new int[1];
            int[] height = new int[1];
            GLFW.glfwGetMonitorWorkarea(monitor, x, y, width, height);
            if (x[0] == primaryX[0] && y[0] == primaryY[0]) continue;
            GLFW.glfwSetWindowPos(window, x[0] + 40, y[0] + 40);
            String result = "muted, input detached; window moved to monitor " + GLFW.glfwGetMonitorName(monitor) + " at " + (x[0] + 40) + "," + (y[0] + 40);
            SmokeTest.LOGGER.info("Smoke client {}", result);
            return result;
        }
        return "muted, input detached; single monitor, window left in place";
    }
}
