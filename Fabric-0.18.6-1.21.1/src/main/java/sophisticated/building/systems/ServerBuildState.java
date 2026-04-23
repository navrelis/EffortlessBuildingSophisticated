package sophisticated.building.systems;

import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.AttachmentHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerBuildState {
    private static final String IS_USING_BUILD_MODE_KEY = SophisticatedBuilding.MODID + ":isUsingBuildMode";
    private static final String IS_QUICK_REPLACING_KEY = SophisticatedBuilding.MODID + ":isQuickReplacing";
    private static final Map<UUID, Boolean> USING_BUILD_MODE = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> QUICK_REPLACING = new ConcurrentHashMap<>();

    public static void handleNewPlayer(Player player) {
        setIsUsingBuildMode(player, false);
        setIsQuickReplacing(player, false);
    }

    public static boolean isUsingBuildMode(Player player) {
        return player != null && USING_BUILD_MODE.getOrDefault(player.getUUID(), false);
    }

    public static void setIsUsingBuildMode(Player player, boolean isUsingBuildMode) {
        if (player == null) {
            return;
        }
        if (isUsingBuildMode) {
            USING_BUILD_MODE.put(player.getUUID(), true);
        } else {
            USING_BUILD_MODE.remove(player.getUUID());
        }
    }

    public static boolean isQuickReplacing(Player player) {
        if (!AttachmentHandler.canReplaceBlocks(player)) return false;
        return player != null && QUICK_REPLACING.getOrDefault(player.getUUID(), false);
    }

    public static void setIsQuickReplacing(Player player, boolean isQuickReplacing) {
        if (player == null) {
            return;
        }
        if (isQuickReplacing) {
            QUICK_REPLACING.put(player.getUUID(), true);
        } else {
            QUICK_REPLACING.remove(player.getUUID());
        }
    }

    public static boolean isLikeVanilla(Player player) {
        return !isUsingBuildMode(player) && !isQuickReplacing(player);
    }
}
