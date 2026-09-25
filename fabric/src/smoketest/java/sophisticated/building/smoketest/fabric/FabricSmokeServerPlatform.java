package sophisticated.building.smoketest.fabric;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import sophisticated.building.smoketest.server.SmokeServerPlatform;
import sophisticated.building.smoketest.server.VanillaFakePlayers;

/** Fabric API 0.42 (Minecraft 1.16.5) has no fake player: a vanilla server player whose connection drops every packet. */
public final class FabricSmokeServerPlatform implements SmokeServerPlatform {
    @Override
    public ServerPlayer createPlayer(ServerLevel level, GameType gameType) {
        return VanillaFakePlayers.create(level, gameType);
    }
}
