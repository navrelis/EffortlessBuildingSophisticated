package sophisticated.building.smoketest.forge;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import sophisticated.building.smoketest.server.SmokeServerPlatform;

import java.util.UUID;

/** Forge's fake player: its connection swallows every packet the mod sends. */
public final class ForgeSmokeServerPlatform implements SmokeServerPlatform {
    @Override
    public ServerPlayer createPlayer(ServerLevel level, GameType gameType) {
        FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "sb-smoketest"));
        player.setGameMode(gameType);
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        return player;
    }
}
