package sophisticated.building.smoketest.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.level.BlockEvent;
import sophisticated.building.smoketest.server.SmokeServerPlatform;
import sophisticated.building.smoketest.server.VanillaFakePlayers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Forge (55, 58) has no fake player API: a vanilla server player whose connection drops every packet. */
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
            // EventBus 7: a predicate listener cancels the event by returning true
            BlockEvent.EntityPlaceEvent.BUS.addListener((BlockEvent.EntityPlaceEvent event) -> REFUSED.contains(event.getPos()));
        }
        REFUSED.clear();
        REFUSED.addAll(positions);
        return true;
    }
}
