package sophisticated.building.smoketest.neoforge.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sophisticated.building.smoketest.client.SmokeWindowHints;

/**
 * Smoke client only (harness mod, never in the release jar): the game window is created without taking the focus from
 * the user ({@link SmokeWindowHints}, set right before glfwCreateWindow, after the game's own hints). The smoke client
 * task turns FML's early loading window off (config/fml.toml earlyWindowControl = false, ../gradle/smoketest.gradle):
 * NeoForge 21.9+ then has no EarlyLoadingScreenController window to take over and creates the window here (vanilla
 * glfwCreateWindow call; the ImmediateWindowHandler of older FML versions is gone).
 */
@Mixin(Window.class)
abstract class SmokeWindowMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false))
    private void sophisticatedbuilding_smoketest$createWithoutFocus(CallbackInfo ci) {
        SmokeWindowHints.beforeWindowCreation();
    }
}
