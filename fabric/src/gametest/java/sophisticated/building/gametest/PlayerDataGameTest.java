package sophisticated.building.gametest;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.platform.Services;

import java.util.UUID;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * Per-player data (power level, the mod's persistent data such as the modifier settings) is saved with the player in
 * the world save, like NeoForge's attachments and persistent data: it comes back after a server restart, a new player
 * object (another world, a restart) starts without it, and it survives death and respawn.
 */
public class PlayerDataGameTest {

    private static final String KEY = "sophisticatedbuilding:gametest";

    //A restart: the server writes the player to its player data file and reads it back into a new player object
    @GameTest
    public void powerLevelAndDataSurviveARestart(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try {
            setPowerLevel(player, 2);
            Services.PLATFORM.getPersistentData(player).putString(KEY, "kept");

            CompoundTag saved = player.saveWithoutId(new CompoundTag());
            //Another player object with nothing in memory: only the save can bring the data back
            saved.remove("UUID");
            ServerPlayer restored = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                    new GameProfile(UUID.randomUUID(), "sb-restored"), ClientInformation.createDefault());
            restored.load(saved);

            expectEquals(helper, "power level after a restart", 2, AttachmentHandler.getPowerLevel(restored));
            expectEquals(helper, "persistent data after a restart", "kept", Services.PLATFORM.getPersistentData(restored).getStringOr(KEY, ""));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    //Singleplayer worlds share one JVM and one player UUID: a new world's player object must not see the last world's data
    @GameTest
    public void dataDoesNotLeakIntoANewPlayerObject(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try {
            setPowerLevel(player, 3);
            Services.PLATFORM.getPersistentData(player).putString(KEY, "old world");

            ServerPlayer sameUuid = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), player.getGameProfile(),
                    ClientInformation.createDefault());
            expectEquals(helper, "power level of a fresh player object with the same UUID", 0, AttachmentHandler.getPowerLevel(sameUuid));
            expectEquals(helper, "persistent data of a fresh player object with the same UUID", "",
                    Services.PLATFORM.getPersistentData(sameUuid).getStringOr(KEY, ""));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    //Death and respawn create a new player object from the old one (ServerPlayer#restoreFrom)
    @GameTest
    public void dataSurvivesRespawn(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try {
            setPowerLevel(player, 1);
            Services.PLATFORM.getPersistentData(player).putString(KEY, "respawned");

            ServerPlayer respawned = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), player.getGameProfile(),
                    ClientInformation.createDefault());
            respawned.restoreFrom(player, false);

            expectEquals(helper, "power level after respawn", 1, AttachmentHandler.getPowerLevel(respawned));
            expectEquals(helper, "persistent data after respawn", "respawned", Services.PLATFORM.getPersistentData(respawned).getStringOr(KEY, ""));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    private static void setPowerLevel(ServerPlayer player, int level) {
        PowerLevel powerLevel = new PowerLevel();
        powerLevel.setPowerLevel(level);
        AttachmentHandler.setPowerLevel(player, powerLevel);
    }
}
