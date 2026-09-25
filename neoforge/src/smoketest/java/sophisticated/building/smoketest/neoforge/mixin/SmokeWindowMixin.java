package sophisticated.building.smoketest.neoforge.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sophisticated.building.smoketest.client.SmokeWindowHints;

/**
 * Smoke client only (harness mod, never in the release jar): the game window is created without taking the focus from
 * the user ({@link SmokeWindowHints}, set right before glfwCreateWindow, after the game's own hints). The smoke client
 * task turns FML's early loading window off (config/fml.toml earlyWindowControl = false, ../gradle/smoketest.gradle):
 * FML then has no EarlyLoadingScreenController window to take over, and the static {@code Window#createGlfwWindow}
 * (26.1+; the constructor before) creates the window with a vanilla glfwCreateWindow call.
 */
@Mixin(Window.class)
abstract class SmokeWindowMixin {

    @Inject(method = "createGlfwWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false))
    private static void sophisticatedbuilding_smoketest$createWithoutFocus(CallbackInfoReturnable<Long> cir) {
        SmokeWindowHints.beforeWindowCreation();
    }
}
