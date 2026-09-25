package sophisticated.building.fabric;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.CommonEvents;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.network.message.ServerConfigSyncPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FabricCommonEvents {
    private static final Map<UUID, ItemStack> LAST_MAIN_HAND = new HashMap<>();

    private FabricCommonEvents() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
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
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                        serverPlayer.inventoryMenu.containerId,
                        serverPlayer.inventoryMenu.incrementStateId(),
                        36 + serverPlayer.getInventory().getSelectedSlot(),
                        serverPlayer.getInventory().getSelectedItem()));
            }
            return InteractionResult.FAIL;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                !CommonEvents.shouldCancelBlockBreak(player));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ServerPlayNetworking.send(player, ServerConfigSyncPacket.fromCurrent());
            CommonEvents.onPlayerLoggedIn(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            LAST_MAIN_HAND.remove(player.getUUID());
            CommonEvents.onPlayerLoggedOut(player);
        });

        // Death, respawn and leaving the End build a new player object: it takes over the mod's data (power level,
        // modifier settings), as NeoForge's Clone event does for the power level
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> FabricPlayerData.copy(oldPlayer, newPlayer));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> CommonEvents.onPlayerRespawned(newPlayer));

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                CommonEvents.onPlayerChangedDimension(player));
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
        if (previous != null && ItemStack.isSameItemSameComponents(previous, held)) {
            return;
        }

        LAST_MAIN_HAND.put(player.getUUID(), held.copy());
        CommonEvents.onMainHandChanged(player, held);
    }
}
