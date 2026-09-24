package sophisticated.building.smoketest.fabric;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import sophisticated.building.smoketest.server.SmokeServerPlatform;

import java.util.UUID;

/** Fabric API's fake player: its connection swallows every packet the mod sends. */
public final class FabricSmokeServerPlatform implements SmokeServerPlatform {
    @Override
    public ServerPlayer createPlayer(ServerLevel level, GameType gameType) {
        FakePlayer player = FakePlayer.get(level, new GameProfile(UUID.randomUUID(), "sb-smoketest"));
        player.setGameMode(gameType);
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        return player;
    }
}
