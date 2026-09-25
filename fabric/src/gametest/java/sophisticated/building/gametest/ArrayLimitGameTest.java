package sophisticated.building.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import sophisticated.building.CommonConfig;
import sophisticated.building.buildmodifier.Array;
import sophisticated.building.network.message.ModifierSettingsPacket;
import sophisticated.building.platform.Services;
import sophisticated.building.utilities.BlockEntry;
import sophisticated.building.utilities.BlockSet;

import java.util.List;

import static sophisticated.building.gametest.GameTestSupport.*;

/**
 * The array modifier stays within the blocks-per-axis limit of the player's power level (its extent, largest offset
 * times count, is what the modifier screen shows against the limit): the client builds at most that many copies, and
 * the server caps the settings it stores (and the mirror radius) the same way.
 */
public class ArrayLimitGameTest {

    private static final String MODIFIERS_KEY = "sophisticatedbuilding:buildModifiers";

    @GameTest
    public void arrayCopiesStopAtTheAxisLimit(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try {
            BlockPos start = helper.absolutePos(new BlockPos(1, 1, 1));
            BlockSet blocks = new BlockSet(List.of(new BlockEntry(start)), start, start, false);
            Array array = new Array();
            array.offset = new Vec3i(1, 0, 0);
            array.count = 20;
            array.findCoordinates(blocks, player);

            int limit = CommonConfig.maxBlocksPerAxis.level0.get();
            expectEquals(helper, "blocks of a 20x array from one block (axis limit " + limit + ")", 1 + limit, blocks.size());
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }

    @GameTest
    public void serverCapsTheStoredModifierSettings(GameTestHelper helper) {
        ServerPlayer player = spawnPlayer(helper, GameType.SURVIVAL);
        try {
            Array array = new Array();
            array.offset = new Vec3i(2, 0, 0);
            array.count = 20;
            CompoundTag mirror = new CompoundTag();
            mirror.putString("type", "Mirror");
            mirror.putBoolean("enabled", true);
            mirror.putInt("radius", 500);
            ListTag list = new ListTag();
            list.add(array.serializeNBT());
            list.add(mirror);
            CompoundTag modifiers = new CompoundTag();
            modifiers.put("modifierSettingsList", list);

            ModifierSettingsPacket.ServerHandler.handleServer(new ModifierSettingsPacket(modifiers), player);

            ListTag stored = Services.PLATFORM.getPersistentData(player).getCompoundOrEmpty(MODIFIERS_KEY).getListOrEmpty("modifierSettingsList");
            int axis = CommonConfig.maxBlocksPerAxis.level0.get();
            expectEquals(helper, "stored array count (offset 2, axis limit " + axis + ")", axis / 2, stored.getCompoundOrEmpty(0).getIntOr("count", -1));
            expectEquals(helper, "stored mirror radius", CommonConfig.maxMirrorRadius.level0.get(), stored.getCompoundOrEmpty(1).getIntOr("radius", -1));
        } finally {
            removePlayer(player);
        }
        helper.succeed();
    }
}
