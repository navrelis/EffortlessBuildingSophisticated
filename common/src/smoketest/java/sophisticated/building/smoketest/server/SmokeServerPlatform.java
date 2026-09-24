package sophisticated.building.smoketest.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ServiceLoader;

/**
 * Loader glue of the server smoke run, registered in {@code META-INF/services} by the loader's smoke source set.
 */
public interface SmokeServerPlatform {

    static SmokeServerPlatform get() {
        return ServiceLoader.load(SmokeServerPlatform.class, SmokeServerPlatform.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException("The loader's smoke source set registers no SmokeServerPlatform"));
    }

    /**
     * A server player without a client (the loader's fake player: its connection swallows the packets the mod sends)
     * in the level, in the given game mode, with an empty inventory and hotbar slot 0 selected.
     */
    ServerPlayer createPlayer(ServerLevel level, GameType gameType);
}
