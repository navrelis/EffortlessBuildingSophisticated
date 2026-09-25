package sophisticated.building.smoketest.forge.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sophisticated.building.smoketest.client.SmokeWindowHints;

/**
 * Smoke client only (harness mod, never in the release jar): the game window is created without taking the focus from
 * the user ({@link SmokeWindowHints}, set right before FML's EarlyProgressVisualization#handOffWindow creates it,
 * after the game's own hints). Forge 39 (and Forge 38 of forge-1.18) has no early loading window (its early progress
 * window is always off), so the window is first created here, and no fml.toml setting is needed.
 */
@Mixin(Window.class)
abstract class SmokeWindowMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/progress/EarlyProgressVisualization;handOffWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J", remap = false))
    private void sophisticatedbuilding_smoketest$createWithoutFocus(CallbackInfo ci) {
        SmokeWindowHints.beforeWindowCreation();
    }
}
