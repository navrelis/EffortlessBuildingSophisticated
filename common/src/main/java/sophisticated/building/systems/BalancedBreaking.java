package sophisticated.building.systems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.message.BalanceStatePacket;
import sophisticated.building.platform.Services;
import sophisticated.building.utilities.BalanceCooldown;
import sophisticated.building.utilities.BalanceRules;

import javax.annotation.Nullable;

/**
 * Server side of balanced survival breaking (rules in {@link BalanceRules}, cooldown in {@link BalanceCooldown}):
 * whether it is on, the request-time checks (cooldown, hunger) that refuse an operation, the cost of a mined block,
 * and the player's cooldown, saved in the per-player data ({@link sophisticated.building.platform.services.IPlatformHelper#getPersistentData})
 * so it survives relog, restart, offline time, death and respawn. The data is read on every use, there is no cache.
 */
public final class BalancedBreaking {

    /** Key of the cooldown ({@link BalanceCooldown#toArray()}, a long array) in the per-player data. */
    public static final String DATA_KEY = SophisticatedBuilding.MODID + ":balanceCooldown";

    //The host's client toggle (ClientConfig.gameplay.balancedBreaking), written by the client every client tick. Only
    //read for an integrated server (same JVM); nothing ever writes it on a dedicated server, so ClientConfig is never
    //loaded there.
    private static volatile boolean hostClientSetting = false;
    //The effective setting the players were last told about (server thread)
    @Nullable
    private static Boolean lastEnabled = null;

    private BalancedBreaking() {
    }

    /** Client: the host's own toggle, for the integrated server it may run. */
    public static void setHostClientSetting(boolean enabled) {
        hostClientSetting = enabled;
    }

    /** Whether balanced breaking is on for the players of this server (see {@link BalanceRules#isEnabled}). */
    public static boolean isEnabled(@Nullable MinecraftServer server) {
        boolean dedicated = server == null || server.isDedicatedServer();
        return BalanceRules.isEnabled(ServerConfig.balancedBreaking.enabled.get(), dedicated, hostClientSetting);
    }

    /** Whether an operation of this player that mines {@code minedBlocks} blocks is balanced. */
    public static boolean appliesTo(Player player, int minedBlocks) {
        return BalanceRules.applies(isEnabled(player.level().getServer()), player.isCreative(), minedBlocks);
    }

    /** Durability uses one mined block costs (1 unless balanced). */
    public static int usesPerBlock(boolean balanced) {
        return BalanceRules.usesPerBlock(balanced, balanced ? ServerConfig.balancedBreaking.durabilityMultiplier.get() : 1);
    }

    /** Hunger exhaustion of one mined block. */
    public static float exhaustionPerBlock(boolean balanced) {
        return balanced ? ServerConfig.balancedBreaking.exhaustionPerBlock.get().floatValue()
                : ServerConfig.survivalBreaking.exhaustionPerBlock.get().floatValue();
    }

    public static BalanceCooldown.Settings settings() {
        ServerConfig.BalancedBreaking config = ServerConfig.balancedBreaking;
        return BalanceRules.settings(config.cooldownBaseSeconds.get(), config.cooldownWindowMinutes.get(), config.cooldownFreeUses.get(),
                config.cooldownStepSeconds.get(), config.cooldownMaxSeconds.get());
    }

    /**
     * A balanced operation arrived: refused (with an action bar message, and the client's pending outline dropped)
     * while the cooldown runs or the player is too hungry; otherwise it counts as one use, which starts the next
     * cooldown at once. Checked and recorded when the request arrives, never when it is applied.
     */
    public static boolean tryStart(Player player, boolean placing) {
        long now = System.currentTimeMillis();
        BalanceCooldown.Settings settings = settings();
        BalanceCooldown cooldown = load(player);
        long remaining = cooldown.remainingMillis(now, settings);
        int minFoodLevel = ServerConfig.balancedBreaking.minFoodLevel.get();
        BalanceRules.Refusal refusal = BalanceRules.check(remaining, player.getFoodData().getFoodLevel(), minFoodLevel);
        if (refusal == BalanceRules.Refusal.COOLDOWN) {
            SophisticatedBuilding.message(player, true, "sophisticatedbuilding.message.balanced_cooldown", BalanceRules.displaySeconds(remaining));
        } else if (refusal == BalanceRules.Refusal.HUNGER) {
            SophisticatedBuilding.message(player, true, "sophisticatedbuilding.message.balanced_too_hungry", minFoodLevel);
        }
        if (refusal != BalanceRules.Refusal.NONE) {
            sendState(player, placing ? BalanceStatePacket.REFUSED_PLACEMENT : BalanceStatePacket.REFUSED_BREAK);
            return false;
        }

        cooldown.recordUse(now, settings);
        save(player, cooldown);
        sendState(player, BalanceStatePacket.REFUSED_NONE);
        return true;
    }

    /** The player's remaining cooldown (also while balanced breaking is off; it only counts while on). */
    public static long remainingMillis(Player player) {
        return load(player).remainingMillis(System.currentTimeMillis(), settings());
    }

    public static BalanceCooldown load(Player player) {
        return BalanceCooldown.fromArray(Services.PLATFORM.getPersistentData(player).getLongArray(DATA_KEY).orElse(new long[0]));
    }

    private static void save(Player player, BalanceCooldown cooldown) {
        Services.PLATFORM.getPersistentData(player).putLongArray(DATA_KEY, cooldown.toArray());
    }

    /**
     * Death, respawn, leaving the End: the new player object takes over the cooldown. For the loaders whose per-player
     * data is not copied as a whole (Forge/NeoForge persistent data: only its "PlayerPersisted" part is copied).
     */
    public static void copyData(Player from, Player to) {
        CompoundTag source = Services.PLATFORM.getPersistentData(from);
        if (source.contains(DATA_KEY)) {
            Services.PLATFORM.getPersistentData(to).putLongArray(DATA_KEY, source.getLongArray(DATA_KEY).orElse(new long[0]));
        }
    }

    public static void sendState(Player player) {
        sendState(player, BalanceStatePacket.REFUSED_NONE);
    }

    private static void sendState(Player player, int refused) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        long now = System.currentTimeMillis();
        BalanceCooldown.Settings settings = settings();
        BalanceCooldown cooldown = load(player);
        boolean enabled = isEnabled(serverPlayer.level().getServer());
        long remaining = enabled ? cooldown.remainingMillis(now, settings) : 0L;
        int uses = cooldown.usesInWindow(now, settings.windowMillis);
        Services.NETWORK.sendToPlayer(serverPlayer, new BalanceStatePacket(enabled, remaining, uses, refused));
    }

    /** Once per server tick: tells every player when the effective setting changed (server config, host toggle). */
    public static void tick(MinecraftServer server) {
        boolean enabled = isEnabled(server);
        if (lastEnabled != null && lastEnabled != enabled) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendState(player);
            }
        }
        lastEnabled = enabled;
    }

    public static void onServerStopped() {
        lastEnabled = null;
    }
}
