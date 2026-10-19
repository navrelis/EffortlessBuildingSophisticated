package sophisticated.building.smoketest.forge.mixin;

import com.mojang.blaze3d.platform.Window;
import net.minecraftforge.fml.loading.progress.EarlyProgressVisualization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sophisticated.building.smoketest.client.SmokeWindowHints;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Smoke client only (harness mod, never in the release jar): the game window is created without taking the focus from
 * the user ({@link SmokeWindowHints}, set right before FML's EarlyProgressVisualization hands the window over, after
 * the game's own hints). Forge 35/36 (Minecraft 1.16) have no fml.toml switch for their early progress window; the
 * smoke client run sets {@code -Dfml.earlyprogresswindow=false}, so handOffWindow creates the window through the game's
 * own glfwCreateWindow call, after this hook, and not before any mod code runs. This is a {@code @Redirect} and not an
 * {@code @Inject}: Forge 36 ships Mixin 0.8.4.12 (patched), whose {@code @At} has no {@code unsafe} attribute, and
 * Mixin 0.8.3+ refuses callback injections into a constructor before RETURN; redirects into constructors are allowed.
 */
@Mixin(Window.class)
abstract class SmokeWindowMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/progress/EarlyProgressVisualization;handOffWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J", remap = false))
    private long sophisticatedbuilding_smoketest$createWithoutFocus(EarlyProgressVisualization visualization, IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        SmokeWindowHints.beforeWindowCreation();
        return visualization.handOffWindow(width, height, title, monitor);
    }
}
