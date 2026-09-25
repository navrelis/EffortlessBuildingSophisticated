package sophisticated.building.attachment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.CommonConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.message.PowerLevelPacket;
import sophisticated.building.platform.Services;

/**
 * Access to the player's {@link PowerLevel}, stored by the loader (see
 * {@link sophisticated.building.platform.services.IPlatformHelper#getPowerLevel}).
 */
public class AttachmentHandler {
    public static final ResourceLocation POWER_LEVEL_CAP = SophisticatedBuilding.asResource("power_level");

    public static PowerLevel getOrCreatePowerLevel(Player player) {
        if (player == null) {
            return new PowerLevel();
        }
        return Services.PLATFORM.getPowerLevel(player);
    }

    public static void setPowerLevel(Player player, PowerLevel powerLevel) {
        if (player == null || powerLevel == null) {
            return;
        }
        Services.PLATFORM.setPowerLevel(player, powerLevel);
    }

    public static boolean hasPowerLevel(Player player) {
        return player != null && Services.PLATFORM.hasPowerLevel(player);
    }

    public static void syncToClient(Player player) {
        PowerLevel powerLevel = getOrCreatePowerLevel(player);
        if (powerLevel == null) return; // Defensive guard during sync

        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            Services.NETWORK.sendToPlayer(serverPlayer, new PowerLevelPacket(powerLevel.getPowerLevel()));
        }
    }

    public static boolean canReplaceBlocks(Player player) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.canReplaceBlocks(player);
            }
        }
        return false;
    }

    public static int getMaxBlocksPerAxis(Player player, boolean nextPowerLevel) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.getMaxBlocksPerAxis(player, nextPowerLevel);
            }
        }
        return CommonConfig.maxBlocksPerAxis.level0.get();
    }

    public static int getMaxBlocksPlacedAtOnce(Player player, boolean nextPowerLevel) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.getMaxBlocksPlacedAtOnce(player, nextPowerLevel);
            }
        }
        return CommonConfig.maxBlocksPlacedAtOnce.level0.get();
    }

    public static int getMaxMirrorRadius(Player player, boolean nextPowerLevel) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.getMaxMirrorRadius(player, nextPowerLevel);
            }
        }
        return CommonConfig.maxMirrorRadius.level0.get();
    }

    public static int getBuildModeReach(Player player) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.getBuildModeReach(player);
            }
        }
        return CommonConfig.maxMirrorRadius.level0.get() + 6;
    }

    public static int getPlacementReach(Player player, boolean nextPowerLevel) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.getPlacementReach(player, nextPowerLevel);
            }
        }
        return CommonConfig.reach.level0.get();
    }

    public static int getPowerLevel(Player player) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.getPowerLevel();
            }
        }
        return 0;
    }

    public static int getNextPowerLevel(Player player) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.getNextPowerLevel();
            }
        }
        return 0;
    }

    public static boolean canIncreasePowerLevel(Player player) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.canIncreasePowerLevel();
            }
        }
        return false;
    }

    public static boolean isDisabled(Player player) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.isDisabled(player);
            }
        }
        return false;
    }

    public static boolean canBreakFar(Player player) {
        if (player != null) {
            PowerLevel powerLevel = getOrCreatePowerLevel(player);
            if (powerLevel != null) {
                return powerLevel.canBreakFar(player);
            }
        }
        return false;
    }
}
