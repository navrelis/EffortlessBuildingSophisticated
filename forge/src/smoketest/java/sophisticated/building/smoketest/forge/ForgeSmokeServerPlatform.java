package sophisticated.building.smoketest.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import sophisticated.building.smoketest.server.SmokeServerPlatform;
import sophisticated.building.smoketest.server.VanillaFakePlayers;

/**
 * Forge 34 (Minecraft 1.16.3) fake players have no connection at all (setGameMode and every packet to them throw), so
 * the smoke server uses the vanilla fake player of the Fabric builds: a server player whose connection drops every
 * packet.
 */
public final class ForgeSmokeServerPlatform implements SmokeServerPlatform {
    @Override
    public ServerPlayer createPlayer(ServerLevel level, GameType gameType) {
        return VanillaFakePlayers.create(level, gameType);
    }
}
