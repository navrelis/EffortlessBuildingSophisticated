package sophisticated.building;

import net.minecraft.core.registries.BuiltInRegistries;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.item.AbstractRandomizerBagItem;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.network.message.BackpackToolsPacket;
import sophisticated.building.network.message.BuildingUpgradeStatePacket;
import sophisticated.building.network.message.ModifierSettingsPacket;
import sophisticated.building.network.message.PowerLevelPacket;
import sophisticated.building.platform.Services;
import sophisticated.building.systems.ServerBuildState;
import sophisticated.building.utilities.BreakToolHelper;
import sophisticated.building.utilities.PowerLevelCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server event logic shared by the loaders. The loader projects call these methods from their own
 * server events; how an event is detected (and e.g. how a placement is cancelled) stays in the loader.
 */
public final class CommonEvents {

    private static final Map<UUID, Map<Item, Integer>> LAST_BACKPACK_COUNTS = new HashMap<>();
    private static final Map<UUID, int[]> LAST_UPGRADE_STATE = new HashMap<>();
    private static final Map<UUID, List<String>> LAST_BACKPACK_TOOLS = new HashMap<>();

    private CommonEvents() {
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        PowerLevelCommand.register(dispatcher);
    }

    /** Once per server tick, for the overworld only. */
    public static void onLevelTick(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) {
            SophisticatedBuilding.SERVER_BLOCK_PLACER.tick();
        }
    }

    public static void onServerStopped() {
        LAST_BACKPACK_COUNTS.clear();
        LAST_UPGRADE_STATE.clear();
        LAST_BACKPACK_TOOLS.clear();
    }

    /**
     * Whether a vanilla block placement by the player must be cancelled: build mode (or quick
     * replace) handles placing while it is active, unless our own block placer is running.
     */
    public static boolean shouldCancelBlockPlace(Player player) {
        if (SophisticatedBuilding.SERVER_BLOCK_PLACER.isPlacingOrBreakingBlocks()) {
            return false;
        }

        //Only cancel if itemblock in hand
        //Fixed issue with e.g. Create Wrench shift-rightclick disassembling being cancelled.
        return !ServerBuildState.isLikeVanilla(player) && isPlayerHoldingBlock(player);
    }

    /**
     * Whether vanilla mining by the player must be cancelled. canBreakFar() also returns true for
     * survival players when survival breaking is enabled (see PowerLevel.canBreakFar /
     * ServerConfig.survivalBreaking), so vanilla mining is cancelled for survival players with a build
     * mode active too, exactly like for creative - build-mode breaking replaces vanilla mining while a
     * build mode is on; switching to Disable mode restores vanilla (design decision D5).
     */
    public static boolean shouldCancelBlockBreak(Player player) {
        if (SophisticatedBuilding.SERVER_BLOCK_PLACER.isPlacingOrBreakingBlocks()) {
            return false;
        }

        PowerLevel powerLevel = AttachmentHandler.getOrCreatePowerLevel(player);
        return !ServerBuildState.isLikeVanilla(player) && powerLevel.canBreakFar(player);
    }

    private static boolean isPlayerHoldingBlock(Player player) {
        ItemStack currentItemStack = player.getMainHandItem();
        return currentItemStack.getItem() instanceof BlockItem
                || (CompatHelper.isItemBlockProxy(currentItemStack) && !player.isShiftKeyDown());
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        ServerBuildState.handleNewPlayer(player);
        Services.NETWORK.sendToPlayer(player, new ModifierSettingsPacket(player));

        PowerLevel powerLevel = AttachmentHandler.getOrCreatePowerLevel(player);
        Services.NETWORK.sendToPlayer(player, new PowerLevelPacket(powerLevel.getPowerLevel()));

        sendBuildingUpgradeState(player, true);
        sendBackpackTools(player, true);
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        SophisticatedBuilding.UNDO_REDO.clear(player);
        LAST_BACKPACK_COUNTS.remove(player.getUUID());
        LAST_UPGRADE_STATE.remove(player.getUUID());
        LAST_BACKPACK_TOOLS.remove(player.getUUID());
    }

    /** After the player respawned (the loader copies the power level first where needed). */
    public static void onPlayerRespawned(ServerPlayer player) {
        ServerBuildState.handleNewPlayer(player);
        LAST_UPGRADE_STATE.remove(player.getUUID());
        LAST_BACKPACK_TOOLS.remove(player.getUUID());
        sendBuildingUpgradeState(player, true);
        sendBackpackTools(player, true);
    }

    public static void onPlayerChangedDimension(ServerPlayer player) {
        //Undo redo has no dimension data, so clear it
        SophisticatedBuilding.UNDO_REDO.clear(player);

        PowerLevel powerLevel = AttachmentHandler.getOrCreatePowerLevel(player);
        Services.NETWORK.sendToPlayer(player, new PowerLevelPacket(powerLevel.getPowerLevel()));

        sendBuildingUpgradeState(player, true);
        sendBackpackTools(player, true);
    }

    /**
     * Once per tick per player: every 10 ticks the backpack counts of the held block (or the held bag's
     * templates), the Building Upgrade state and the backpack tools are re-sent when they changed.
     */
    public static void onPlayerTick(ServerPlayer player) {
        syncPeriodicBackpackCounts(player);
        if (player.tickCount % 10 == 0) {
            sendBuildingUpgradeState(player, false);
            sendBackpackTools(player, false);
        }
    }

    /** The item in the player's main hand changed: sync the backpack counts of what it places. */
    public static void onMainHandChanged(ServerPlayer player, ItemStack held) {
        if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
            return;
        }

        try {
            if (held.getItem() instanceof BlockItem) {
                syncItemCount(player, held.getItem());
            } else if (held.getItem() instanceof AbstractRandomizerBagItem bagItem) {
                IItemHandler bagInventory = bagItem.getBagInventory(held);
                if (bagInventory != null) {
                    List<ItemStack> templates = bagItem.getTemplates(bagInventory);
                    for (ItemStack template : templates) {
                        syncItemCount(player, template.getItem());
                    }
                }
            }
        } catch (Exception | LinkageError ignored) {
            // Optional SophisticatedBackpacks integration missing.
        }
    }

    /**
     * Syncs the count of a specific item in the player's backpacks to the client: the unclamped total
     * across every backpack with an enabled upgrade (RC2/T4). Extraction is separately capped to the
     * effective building upgrade limit.
     */
    private static void syncItemCount(ServerPlayer player, Item item) {
        int count = Services.backpacks().countBlockInBackpacksForDisplay(player, new ItemStack(item));
        Services.NETWORK.sendToPlayer(player, new BackpackItemCountPacket(BuiltInRegistries.ITEM.getKey(item), count));
    }

    private static void syncPeriodicBackpackCounts(ServerPlayer player) {
        if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
            return;
        }

        // Only sync periodically to keep network traffic low
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
                // Unclamped total across every backpack with an enabled upgrade (RC2/T4).
                int count = Services.backpacks().countBlockInBackpacksForDisplay(player, new ItemStack(item));
                Integer last = lastCounts.get(item);
                if (last == null || last != count) {
                    lastCounts.put(item, count);
                    Services.NETWORK.sendToPlayer(player, new BackpackItemCountPacket(BuiltInRegistries.ITEM.getKey(item), count));
                }
            }

            // Drop stale entries to keep the cache compact
            lastCounts.keySet().removeIf(item -> !itemsToSync.contains(item));
        } catch (Exception | LinkageError ignored) {
            // Optional SophisticatedBackpacks integration missing.
        }
    }

    /**
     * Sends the tools found inside an enabled Tool Swapper / Advanced Tool Swapper backpack
     * upgrade to the client, for the survival-breaking preview/HUD (T-S4/T-S8). Sent
     * unconditionally on join/respawn/dimension change ({@code force}); from the periodic tick it
     * is only sent when the fingerprint (item id + damage + count per tool, in order) differs from
     * the last value sent to that player.
     */
    private static void sendBackpackTools(ServerPlayer player, boolean force) {
        if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
            return;
        }

        try {
            List<BreakToolHelper.ToolSlot> slots = Services.backpacks().collectBackpackTools(player);
            List<ItemStack> tools = new ArrayList<>(slots.size());
            List<String> fingerprint = new ArrayList<>(slots.size());
            for (BreakToolHelper.ToolSlot slot : slots) {
                ItemStack stack = slot.get().copy();
                tools.add(stack);
                fingerprint.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + "#" + stack.getDamageValue() + "#" + stack.getCount());
            }

            List<String> last = LAST_BACKPACK_TOOLS.get(player.getUUID());
            if (!force && fingerprint.equals(last)) {
                return;
            }

            LAST_BACKPACK_TOOLS.put(player.getUUID(), fingerprint);
            Services.NETWORK.sendToPlayer(player, new BackpackToolsPacket(tools));
        } catch (Exception | LinkageError e) {
            SophisticatedBuilding.logger.debug("Error syncing backpack tools: {}", e.getMessage());
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
            int tier = Services.backpacks().getBuildingUpgradeTier(player);
            int maxBlocks = Services.backpacks().getMaxBlocksForPlayer(player);

            int[] last = LAST_UPGRADE_STATE.get(player.getUUID());
            if (!force && last != null && last[0] == tier && last[1] == maxBlocks) {
                return;
            }

            LAST_UPGRADE_STATE.put(player.getUUID(), new int[] { tier, maxBlocks });
            Services.NETWORK.sendToPlayer(player, new BuildingUpgradeStatePacket(tier, maxBlocks));
        } catch (Exception | LinkageError ignored) {
            // Optional SophisticatedBackpacks integration missing.
        }
    }
}
