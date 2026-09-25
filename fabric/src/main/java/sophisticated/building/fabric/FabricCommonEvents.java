package sophisticated.building.fabric;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.CommonEvents;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.network.message.ServerConfigSyncPacket;
import sophisticated.building.platform.Services;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FabricCommonEvents {
    private static final Map<UUID, ItemStack> LAST_MAIN_HAND = new HashMap<>();

    private FabricCommonEvents() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                CommonEvents.registerCommands(dispatcher));

        ServerTickEvents.END_WORLD_TICK.register(CommonEvents::onLevelTick);
        ServerTickEvents.END_SERVER_TICK.register(FabricCommonEvents::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LAST_MAIN_HAND.clear();
            CommonEvents.onServerStopped();
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (!CommonEvents.shouldCancelBlockPlace(player)) {
                return InteractionResult.PASS;
            }
            // Like NeoForge/Forge: the client already predicted the placement and took the item from the selected
            // slot, and the server never tells it otherwise. Resend the slot, or a player holding exactly the items
            // the build needs (e.g. 1 block + a backpack with a Building Upgrade) loses the held block client side.
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                        serverPlayer.inventoryMenu.containerId,
                        36 + serverPlayer.inventory.selected,
                        serverPlayer.inventory.getSelected()));
            }
            return InteractionResult.FAIL;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !CommonEvents.shouldCancelBlockBreak(player));

    }

    // Fabric API 0.25.0 (Minecraft 1.16.3) has no player connection, respawn or world change events (networking v1 and
    // entity events v1 arrive later): the mixins in sophisticated.building.fabric.mixin call the four methods below at
    // the places those events fire in later Fabric API versions.

    /** A player joined the server (PlayerList.placeNewPlayer). */
    public static void onPlayerJoin(ServerPlayer player) {
        Services.NETWORK.sendToPlayer(player, ServerConfigSyncPacket.fromCurrent());
        CommonEvents.onPlayerLoggedIn(player);
    }

    /** A player left the server (PlayerList.remove). */
    public static void onPlayerDisconnect(ServerPlayer player) {
        LAST_MAIN_HAND.remove(player.getUUID());
        CommonEvents.onPlayerLoggedOut(player);
    }

    /** The player respawned or returned from the End (PlayerList.respawn); newPlayer replaces oldPlayer. */
    public static void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
        if (AttachmentHandler.hasPowerLevel(oldPlayer)) {
            AttachmentHandler.setPowerLevel(newPlayer, AttachmentHandler.getOrCreatePowerLevel(oldPlayer));
        }
        CommonEvents.onPlayerRespawned(newPlayer);
    }

    /** The player moved to another dimension (portal or cross-dimension teleport). */
    public static void onPlayerChangedWorld(ServerPlayer player) {
        CommonEvents.onPlayerChangedDimension(player);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncOnHeldItemChange(player);
            CommonEvents.onPlayerTick(player);
        }
    }

    // Fabric has no equipment change event: compare the main hand item every tick.
    private static void syncOnHeldItemChange(ServerPlayer player) {
        if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        ItemStack previous = LAST_MAIN_HAND.get(player.getUUID());
        if (previous != null && (ItemStack.isSame(previous, held) && ItemStack.tagMatches(previous, held))) {
            return;
        }

        LAST_MAIN_HAND.put(player.getUUID(), held.copy());
        CommonEvents.onMainHandChanged(player, held);
    }
}
