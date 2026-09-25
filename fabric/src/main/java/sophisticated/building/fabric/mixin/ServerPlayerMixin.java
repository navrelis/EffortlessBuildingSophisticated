package sophisticated.building.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sophisticated.building.fabric.FabricCommonEvents;

/**
 * Dimension changes on Minecraft 1.16.3, where Fabric API 0.25.0 has no ServerEntityWorldChangeEvents (see
 * {@link FabricCommonEvents}): through a portal (changeDimension) or a cross-dimension teleport (teleportTo), like
 * AFTER_PLAYER_CHANGE_WORLD of later Fabric API versions. Fired only when the player's level really changed.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Unique
    private ServerLevel sophisticatedbuilding$originLevel;

    @Inject(method = "changeDimension", at = @At("HEAD"))
    private void sophisticatedbuilding$beforeChangeDimension(ServerLevel destination, CallbackInfoReturnable<Entity> cir) {
        sophisticatedbuilding$originLevel = self().getLevel();
    }

    @Inject(method = "changeDimension", at = @At("RETURN"))
    private void sophisticatedbuilding$afterChangeDimension(ServerLevel destination, CallbackInfoReturnable<Entity> cir) {
        sophisticatedbuilding$fireIfLevelChanged();
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", at = @At("HEAD"))
    private void sophisticatedbuilding$beforeTeleport(ServerLevel destination, double x, double y, double z, float yRot, float xRot, CallbackInfo ci) {
        sophisticatedbuilding$originLevel = self().getLevel();
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", at = @At("RETURN"))
    private void sophisticatedbuilding$afterTeleport(ServerLevel destination, double x, double y, double z, float yRot, float xRot, CallbackInfo ci) {
        sophisticatedbuilding$fireIfLevelChanged();
    }

    @Unique
    private void sophisticatedbuilding$fireIfLevelChanged() {
        ServerLevel origin = sophisticatedbuilding$originLevel;
        sophisticatedbuilding$originLevel = null;
        if (origin != null && origin != self().getLevel()) {
            FabricCommonEvents.onPlayerChangedWorld(self());
        }
    }

    @Unique
    private ServerPlayer self() {
        return (ServerPlayer) (Object) this;
    }
}
