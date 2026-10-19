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
 * task turns FML's early loading window off (config/fml.toml earlyWindowControl = false, ../gradle/smoketest.gradle), so
 * NeoForge 21.5+ does not take over the early window (EarlyLoadingScreenController) and creates it here.
 * {@code unsafe = true} is required because Mixin 0.8.3+ refuses callback injections into a constructor before RETURN.
 */
@Mixin(Window.class)
abstract class SmokeWindowMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false, unsafe = true))
    private void sophisticatedbuilding_smoketest$createWithoutFocus(CallbackInfo ci) {
        SmokeWindowHints.beforeWindowCreation();
    }
}
