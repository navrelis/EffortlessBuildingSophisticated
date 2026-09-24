package sophisticated.building.forge.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sophisticated.building.forge.SophisticatedBuildingForgeClient;

/**
 * Forge 51 (Minecraft 1.21) has no HUD layer event: the mod's HUD is drawn after vanilla's whole HUD.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void sophisticatedbuilding$renderHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SophisticatedBuildingForgeClient.renderHud(guiGraphics, deltaTracker);
    }
}
