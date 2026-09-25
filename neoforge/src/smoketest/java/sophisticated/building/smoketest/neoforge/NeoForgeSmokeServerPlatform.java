package sophisticated.building.smoketest.neoforge;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import sophisticated.building.smoketest.server.SmokeServerPlatform;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** NeoForge's fake player: its connection swallows every packet the mod sends. */
public final class NeoForgeSmokeServerPlatform implements SmokeServerPlatform {
    private static final Set<BlockPos> REFUSED = ConcurrentHashMap.newKeySet();
    private static boolean listening;

    @Override
    public ServerPlayer createPlayer(ServerLevel level, GameType gameType) {
        FakePlayer player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "sb-smoketest"));
        player.setGameMode(gameType);
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        return player;
    }

    @Override
    public synchronized boolean refusePlacementsAt(Set<BlockPos> positions) {
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> {
                if (REFUSED.contains(event.getPos())) event.setCanceled(true);
            });
        }
        REFUSED.clear();
        REFUSED.addAll(positions);
        return true;
    }
}
