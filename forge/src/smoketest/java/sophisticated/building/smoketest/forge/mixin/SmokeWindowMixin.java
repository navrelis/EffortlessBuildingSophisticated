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
 * the user ({@link SmokeWindowHints}, set right before Forge 43's EarlyProgressVisualization creates (or hands over)
 * the window, after the game's own hints). The Window constructor of Forge 43 has no glfwCreateWindow call of its own,
 * it calls EarlyProgressVisualization.handOffWindow instead, which is the call redirected here. The smoke client task
 * writes earlyWindowControl = false into the game dir's config/fml.toml before every launch and keeps the other
 * settings of that file (../gradle/smoketest.gradle), so the game creates its window itself, through the Window
 * constructor, and not FML's early loading window before any mod code runs.
 * This is a {@code @Redirect} and not an {@code @Inject}: Forge 43 ships Mixin 0.8.5, whose {@code @At} has no
 * {@code unsafe} attribute, and Mixin 0.8.3+ refuses callback injections into a constructor before RETURN; redirects
 * into constructors are allowed.
 */
@Mixin(Window.class)
abstract class SmokeWindowMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/progress/EarlyProgressVisualization;handOffWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J", remap = false))
    private long sophisticatedbuilding_smoketest$createWithoutFocus(EarlyProgressVisualization visualization, IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        SmokeWindowHints.beforeWindowCreation();
        return visualization.handOffWindow(width, height, title, monitor);
    }
}
