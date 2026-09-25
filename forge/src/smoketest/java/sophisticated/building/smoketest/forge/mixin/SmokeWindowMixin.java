package sophisticated.building.smoketest.forge.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sophisticated.building.smoketest.client.SmokeWindowHints;

/**
 * Smoke client only (harness mod, never in the release jar): the game window is created without taking the focus from
 * the user ({@link SmokeWindowHints}, set right before FML's EarlyProgressVisualization hands the window over, after
 * the game's own hints). Forge 35/36 (Minecraft 1.16) have no fml.toml switch for their early progress window; the
 * smoke client run sets {@code -Dfml.earlyprogresswindow=false}, so handOffWindow creates the window through the game's
 * own glfwCreateWindow call, after this hook, and not before any mod code runs.
 */
@Mixin(Window.class)
abstract class SmokeWindowMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/progress/EarlyProgressVisualization;handOffWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J", remap = false))
    private void sophisticatedbuilding_smoketest$createWithoutFocus(CallbackInfo ci) {
        SmokeWindowHints.beforeWindowCreation();
    }
}
