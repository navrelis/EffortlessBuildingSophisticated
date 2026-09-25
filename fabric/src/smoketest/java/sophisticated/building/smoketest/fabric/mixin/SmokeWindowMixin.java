package sophisticated.building.smoketest.fabric.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sophisticated.building.smoketest.SmokeTest;

/**
 * Smoke client only (harness mod, never in the release jar): the game window is created without taking the focus from
 * the user (GLFW hints FOCUSED and FOCUS_ON_SHOW off, set right before glfwCreateWindow, after the game's own hints).
 * An unfocused window never grabs the mouse; ClientWindow keeps it that way for the rest of the run.
 */
@Mixin(Window.class)
abstract class SmokeWindowMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false))
    private void sophisticatedbuilding_smoketest$createWithoutFocus(CallbackInfo ci) {
        if (SmokeTest.isClientMode()) {
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        }
    }
}
