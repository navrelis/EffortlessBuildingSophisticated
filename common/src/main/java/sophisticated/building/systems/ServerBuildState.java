package sophisticated.building.systems;

import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.platform.Services;

/**
 * Whether a player is using a build mode / quick replace, kept in the loader's per-player data (see
 * {@link sophisticated.building.platform.services.IPlatformHelper#getPersistentData}). The client
 * re-sends both on join (BuildModes.resyncToServer(), see T-S6).
 */
public class ServerBuildState {
    private static final String IS_USING_BUILD_MODE_KEY = SophisticatedBuilding.MODID + ":isUsingBuildMode";
    private static final String IS_QUICK_REPLACING_KEY = SophisticatedBuilding.MODID + ":isQuickReplacing";

    public static void handleNewPlayer(Player player) {
        setIsUsingBuildMode(player, false);
        setIsQuickReplacing(player, false);
    }

    public static boolean isUsingBuildMode(Player player) {
        return player != null && Services.PLATFORM.getPersistentData(player).contains(IS_USING_BUILD_MODE_KEY);
    }

    public static void setIsUsingBuildMode(Player player, boolean isUsingBuildMode) {
        if (player == null) {
            return;
        }
        if (isUsingBuildMode) {
            Services.PLATFORM.getPersistentData(player).putBoolean(IS_USING_BUILD_MODE_KEY, true);
        } else {
            Services.PLATFORM.getPersistentData(player).remove(IS_USING_BUILD_MODE_KEY);
        }
    }

    public static boolean isQuickReplacing(Player player) {
        if (!AttachmentHandler.canReplaceBlocks(player)) return false;
        return player != null && Services.PLATFORM.getPersistentData(player).contains(IS_QUICK_REPLACING_KEY);
    }

    public static void setIsQuickReplacing(Player player, boolean isQuickReplacing) {
        if (player == null) {
            return;
        }
        if (isQuickReplacing) {
            Services.PLATFORM.getPersistentData(player).putBoolean(IS_QUICK_REPLACING_KEY, true);
        } else {
            Services.PLATFORM.getPersistentData(player).remove(IS_QUICK_REPLACING_KEY);
        }
    }

    public static boolean isLikeVanilla(Player player) {
        return !isUsingBuildMode(player) && !isQuickReplacing(player);
    }
}
