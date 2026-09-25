package sophisticated.building.fabric.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sophisticated.building.fabric.FabricCommonEvents;

/**
 * Player join, leave and respawn on Minecraft 1.16.3, where Fabric API 0.25.0 has no ServerPlayConnectionEvents /
 * ServerPlayerEvents (see {@link FabricCommonEvents}). Same places as those events in later Fabric API versions.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void sophisticatedbuilding$afterJoin(Connection connection, ServerPlayer player, CallbackInfo ci) {
        FabricCommonEvents.onPlayerJoin(player);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void sophisticatedbuilding$beforeRemove(ServerPlayer player, CallbackInfo ci) {
        FabricCommonEvents.onPlayerDisconnect(player);
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void sophisticatedbuilding$afterRespawn(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfoReturnable<ServerPlayer> cir) {
        FabricCommonEvents.onPlayerRespawn(oldPlayer, cir.getReturnValue());
    }
}
