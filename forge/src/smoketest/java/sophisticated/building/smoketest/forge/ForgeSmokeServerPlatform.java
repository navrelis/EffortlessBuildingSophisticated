package sophisticated.building.smoketest.forge;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import sophisticated.building.smoketest.server.SmokeServerPlatform;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge's fake player: its connection swallows every packet the mod sends. Forge 41 (forge-1.19) reports every fake
 * player at 0, 0, 0 ({@code FakePlayer#position} / {@code blockPosition}), so the server's reach check of build requests
 * would refuse everything the scenarios send from their test structure: the harness player reports its real position
 * (on Forge 43 FakePlayer does not override them, and this changes nothing).
 */
public final class ForgeSmokeServerPlatform implements SmokeServerPlatform {
    private static final Set<BlockPos> REFUSED = ConcurrentHashMap.newKeySet();
    private static boolean listening;

    @Override
    public ServerPlayer createPlayer(ServerLevel level, GameType gameType) {
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "sb-smoketest")) {
            @Override
            public Vec3 position() {
                return new Vec3(getX(), getY(), getZ());
            }

            @Override
            public BlockPos blockPosition() {
                return new BlockPos(getX(), getY(), getZ());
            }
        };
        player.setGameMode(gameType);
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        return player;
    }

    @Override
    public synchronized boolean refusePlacementsAt(Set<BlockPos> positions) {
        if (!listening) {
            listening = true;
            MinecraftForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent event) -> {
                if (REFUSED.contains(event.getPos())) event.setCanceled(true);
            });
        }
        REFUSED.clear();
        REFUSED.addAll(positions);
        return true;
    }
}
