package sophisticated.building.fabric;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.item.AbstractRandomizerBagItem;
import sophisticated.building.item.upgrade.BuildingUpgradeHelper;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.network.message.BuildingUpgradeStatePacket;
import sophisticated.building.network.message.ModifierSettingsPacket;
import sophisticated.building.network.message.PowerLevelPacket;
import sophisticated.building.systems.ServerBuildState;
import sophisticated.building.utilities.PowerLevelCommand;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FabricCommonEvents {
    private static final Map<UUID, ItemStack> LAST_MAIN_HAND = new HashMap<>();
    private static final Map<UUID, Map<Item, Integer>> LAST_BACKPACK_COUNTS = new HashMap<>();
    private static final Map<UUID, int[]> LAST_UPGRADE_STATE = new HashMap<>();

    private FabricCommonEvents() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                PowerLevelCommand.register(dispatcher));

        ServerTickEvents.END_WORLD_TICK.register(FabricCommonEvents::onWorldTick);
        ServerTickEvents.END_SERVER_TICK.register(FabricCommonEvents::onServerTick);
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (CompatHelper.isCatnipLoaded()) {
                sophisticated.building.create.events.CommonEvents.onUnloadWorld(world);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LAST_MAIN_HAND.clear();
            LAST_BACKPACK_COUNTS.clear();
            LAST_UPGRADE_STATE.clear();
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (SophisticatedBuilding.SERVER_BLOCK_PLACER.isPlacingOrBreakingBlocks()) {
                return InteractionResult.PASS;
            }

            if (!ServerBuildState.isLikeVanilla(player) && isPlayerHoldingBlock(player)) {
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (SophisticatedBuilding.SERVER_BLOCK_PLACER.isPlacingOrBreakingBlocks()) {
                return true;
            }

            PowerLevel powerLevel = AttachmentHandler.getOrCreatePowerLevel(player);
            return ServerBuildState.isLikeVanilla(player) || !powerLevel.canBreakFar(player);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ServerBuildState.handleNewPlayer(player);
            ServerPlayNetworking.send(player, new ModifierSettingsPacket(player));

            PowerLevel powerLevel = AttachmentHandler.getOrCreatePowerLevel(player);
            ServerPlayNetworking.send(player, new PowerLevelPacket(powerLevel.getPowerLevel()));

            sendBuildingUpgradeState(player, true);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            SophisticatedBuilding.UNDO_REDO.clear(player);
            LAST_MAIN_HAND.remove(player.getUUID());
            LAST_BACKPACK_COUNTS.remove(player.getUUID());
            LAST_UPGRADE_STATE.remove(player.getUUID());
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            ServerBuildState.handleNewPlayer(newPlayer);
            if (AttachmentHandler.hasPowerLevel(oldPlayer)) {
                AttachmentHandler.setPowerLevel(newPlayer, AttachmentHandler.getOrCreatePowerLevel(oldPlayer));
            }
            LAST_UPGRADE_STATE.remove(oldPlayer.getUUID());
            sendBuildingUpgradeState(newPlayer, true);
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            SophisticatedBuilding.UNDO_REDO.clear(player);
            PowerLevel powerLevel = AttachmentHandler.getOrCreatePowerLevel(player);
            ServerPlayNetworking.send(player, new PowerLevelPacket(powerLevel.getPowerLevel()));

            sendBuildingUpgradeState(player, true);
        });
    }

    private static void onWorldTick(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) {
            SophisticatedBuilding.SERVER_BLOCK_PLACER.tick();
        }
    }

    private static void onServerTick(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncOnHeldItemChange(player);
            syncPeriodicBackpackCounts(player);
            if (player.tickCount % 10 == 0) {
                sendBuildingUpgradeState(player, false);
            }
        }
    }

    /**
     * Sends the player's current Building Upgrade tier/maxBlocks to the client (RC2). Sent
     * unconditionally on join/respawn/dimension change ({@code force}); from the periodic tick it
     * is only sent when the (tier, maxBlocks) pair differs from the last value sent to that
     * player.
     */
    private static void sendBuildingUpgradeState(ServerPlayer player, boolean force) {
        if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
            return;
        }

        try {
            int tier = BuildingUpgradeHelper.getBuildingUpgradeTier(player);
            int maxBlocks = BuildingUpgradeHelper.getMaxBlocksForPlayer(player);

            int[] last = LAST_UPGRADE_STATE.get(player.getUUID());
            if (!force && last != null && last[0] == tier && last[1] == maxBlocks) {
                return;
            }

            LAST_UPGRADE_STATE.put(player.getUUID(), new int[] { tier, maxBlocks });
            ServerPlayNetworking.send(player, new BuildingUpgradeStatePacket(tier, maxBlocks));
        } catch (NoClassDefFoundError ignored) {
            // Optional SophisticatedBackpacks integration missing.
        }
    }

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

        try {
            if (held.getItem() instanceof BlockItem) {
                BuildingUpgradeHelper.syncItemCount(player, held.getItem());
            } else if (held.getItem() instanceof AbstractRandomizerBagItem bagItem) {
                IItemHandler bagInventory = bagItem.getBagInventory(held);
                if (bagInventory != null) {
                    List<ItemStack> templates = bagItem.getTemplates(bagInventory);
                    for (ItemStack template : templates) {
                        BuildingUpgradeHelper.syncItemCount(player, template.getItem());
                    }
                }
            }
        } catch (NoClassDefFoundError ignored) {
            // Optional SophisticatedBackpacks integration missing.
        }
    }

    private static void syncPeriodicBackpackCounts(ServerPlayer player) {
        if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
            return;
        }

        if (player.tickCount % 10 != 0) {
            return;
        }

        try {
            Set<Item> itemsToSync = new LinkedHashSet<>();
            ItemStack held = player.getMainHandItem();

            if (held.getItem() instanceof BlockItem) {
                itemsToSync.add(held.getItem());
            } else if (held.getItem() instanceof AbstractRandomizerBagItem bagItem) {
                IItemHandler bagInventory = bagItem.getBagInventory(held);
                if (bagInventory != null) {
                    List<ItemStack> templates = bagItem.getTemplates(bagInventory);
                    for (ItemStack template : templates) {
                        itemsToSync.add(template.getItem());
                    }
                }
            }

            if (itemsToSync.isEmpty()) {
                return;
            }

            Map<Item, Integer> lastCounts = LAST_BACKPACK_COUNTS.computeIfAbsent(player.getUUID(), key -> new HashMap<>());
            for (Item item : itemsToSync) {
                int count = BuildingUpgradeHelper.countBlockInBackpack(player, new ItemStack(item));
                Integer last = lastCounts.get(item);
                if (last == null || last != count) {
                    lastCounts.put(item, count);
                    ServerPlayNetworking.send(player, new BackpackItemCountPacket(BuiltInRegistries.ITEM.getKey(item), count));
                }
            }

            lastCounts.keySet().removeIf(item -> !itemsToSync.contains(item));
        } catch (NoClassDefFoundError ignored) {
            // Optional SophisticatedBackpacks integration missing.
        }
    }

    private static boolean isPlayerHoldingBlock(net.minecraft.world.entity.player.Player player) {
        ItemStack currentItemStack = player.getMainHandItem();
        return currentItemStack.getItem() instanceof BlockItem
                || (CompatHelper.isItemBlockProxy(currentItemStack) && !player.isShiftKeyDown());
    }
}
