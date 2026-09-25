package sophisticated.building.smoketest.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import sophisticated.building.smoketest.server.SmokeServerPlatform;
import sophisticated.building.smoketest.server.VanillaFakePlayers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The vanilla fake player of the Fabric builds (a server player whose connection drops every packet), not Forge's
 * FakePlayer: Forge 36's FakePlayer reports its position as the world origin (blockPosition() and position() are
 * overridden), so the server's reach check of build requests would refuse every request of a player standing at the
 * test structure.
 */
public final class ForgeSmokeServerPlatform implements SmokeServerPlatform {
    private static final Set<BlockPos> REFUSED = ConcurrentHashMap.newKeySet();
    private static boolean listening;

    @Override
    public ServerPlayer createPlayer(ServerLevel level, GameType gameType) {
        return VanillaFakePlayers.create(level, gameType);
    }

    @Override
    public synchronized boolean refusePlacementsAt(Set<BlockPos> positions) {
        if (!listening) {
            listening = true;
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, BlockEvent.EntityPlaceEvent.class, event -> {
                if (REFUSED.contains(event.getPos())) event.setCanceled(true);
            });
        }
        REFUSED.clear();
        REFUSED.addAll(positions);
        return true;
    }
}
