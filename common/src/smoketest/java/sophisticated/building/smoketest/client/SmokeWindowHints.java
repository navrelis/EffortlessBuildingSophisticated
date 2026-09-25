package sophisticated.building.smoketest.client;

import org.lwjgl.glfw.GLFW;
import sophisticated.building.smoketest.SmokeTest;

/**
 * GLFW hints for the smoke client's game window, set by each loader's harness mixin (SmokeWindowMixin) right before the
 * window is created, after the game's own hints: the window is created without the focus and does not take it when
 * shown, so a smoke run never takes the keyboard focus from the user. An unfocused window never grabs the mouse;
 * {@link ClientWindow} keeps it that way for the rest of the run.
 */
public final class SmokeWindowHints {

    private SmokeWindowHints() {
    }

    public static void beforeWindowCreation() {
        if (!SmokeTest.isClientMode()) return;
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
    }
}
