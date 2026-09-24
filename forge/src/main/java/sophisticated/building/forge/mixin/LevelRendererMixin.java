package sophisticated.building.forge.mixin;

import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sophisticated.building.forge.ForgeClientEvents;

/**
 * Forge 55 (Minecraft 1.21.5) has no event to render into the level: the world previews get their own frame pass,
 * added after the last vanilla pass ({@code late_debug}) and before the frame graph runs.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Inject(method = "addLateDebugPass", at = @At("TAIL"))
    private void sophisticatedbuilding$addPreviewPass(FrameGraphBuilder frameGraph, Vec3 cameraPosition, FogParameters fog, CallbackInfo ci) {
        ForgeClientEvents.addPreviewPass(frameGraph, targets);
    }
}
