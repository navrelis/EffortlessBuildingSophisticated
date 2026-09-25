package sophisticated.building.smoketest.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import sophisticated.building.smoketest.SmokeTest;

import java.util.Set;

/**
 * Loader glue of the server smoke run, registered in {@code META-INF/services} by the loader's smoke source set.
 */
public interface SmokeServerPlatform {

    static SmokeServerPlatform get() {
        return SmokeTest.firstService(SmokeServerPlatform.class)
                .orElseThrow(() -> new IllegalStateException("The loader's smoke source set registers no SmokeServerPlatform"));
    }

    /**
     * A server player without a client (the loader's fake player: its connection swallows the packets the mod sends)
     * in the level, in the given game mode, with an empty inventory and hotbar slot 0 selected.
     */
    ServerPlayer createPlayer(ServerLevel level, GameType gameType);

    /**
     * From now on the loader's block place event refuses every placement at these positions, as a protection mod does
     * (an empty set ends it). False if the loader fires no such event for the mod's placements (Fabric).
     */
    default boolean refusePlacementsAt(Set<BlockPos> positions) {
        return false;
    }
}
