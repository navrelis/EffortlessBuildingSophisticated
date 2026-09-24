package sophisticated.building.forge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sophisticated.building.render.RenderHandler;

/**
 * Minecraft 26.2 has no immediate drawing in the level render any more (no {@code MultiBufferSource}), and Forge 65 has
 * no event while the level's submits are collected (its frame pass event runs when the submits are already prepared).
 * The block previews, mirror/array lines, ghost blocks and outlines are submitted at the end of
 * {@code LevelRenderer#submitFeatures}, where Fabric's and NeoForge's custom geometry events fire, with a fresh pose
 * stack like the one vanilla submits the entities with.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Inject(method = "submitFeatures", at = @At("TAIL"))
    private void sophisticatedbuilding$submitPreviews(LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector,
                                                      boolean renderOutline, CallbackInfo ci) {
        RenderHandler.onSubmitLevel(new PoseStack(), submitNodeCollector);
    }
}
