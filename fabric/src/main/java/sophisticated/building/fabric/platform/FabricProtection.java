package sophisticated.building.fabric.platform;

import eu.pb4.common.protection.api.CommonProtection;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import sophisticated.building.SophisticatedBuilding;

/**
 * The Common Protection API (Patbox, mod id {@code common-protection-api}), which claim and protection mods ship and
 * register a provider with (e.g. Patbox's GOML Reserved; any claim mod that registers a provider is covered). Optional:
 * when no installed mod ships it, every check passes. Only this class touches the API, and only after the mod check, so a
 * missing API never loads its classes.
 */
final class FabricProtection {

    private static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("common-protection-api");
    private static boolean failed;

    private FabricProtection() {
    }

    static boolean canPlace(Player player, Level level, BlockPos pos) {
        if (!PRESENT || failed) return true;
        try {
            return CommonProtection.canPlaceBlock(level, pos, player.getGameProfile(), player);
        } catch (Exception | LinkageError e) {
            failed = true;
            SophisticatedBuilding.logger.warn("Common Protection API check failed, placements are no longer checked with it", e);
            return true;
        }
    }

    static boolean canBreak(Player player, Level level, BlockPos pos) {
        if (!PRESENT || failed) return true;
        try {
            return CommonProtection.canBreakBlock(level, pos, player.getGameProfile(), player);
        } catch (Exception | LinkageError e) {
            failed = true;
            SophisticatedBuilding.logger.warn("Common Protection API check failed, breaks are no longer checked with it", e);
            return true;
        }
    }
}
