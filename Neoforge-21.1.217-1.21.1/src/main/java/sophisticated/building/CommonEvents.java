package sophisticated.building;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.integration.ToolSwapperIntegration;
import sophisticated.building.item.upgrade.BuildingUpgradeItem;
import sophisticated.building.network.message.BackpackToolsPacket;
import sophisticated.building.network.message.ModifierSettingsPacket;
import sophisticated.building.network.message.PowerLevelPacket;
import sophisticated.building.systems.ServerBuildState;
import sophisticated.building.utilities.BreakToolHelper;
import sophisticated.building.utilities.PowerLevelCommand;

import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import sophisticated.building.item.upgrade.BuildingUpgradeHelper;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import sophisticated.building.item.AbstractRandomizerBagItem;
import net.minecraft.core.registries.BuiltInRegistries;
import sophisticated.building.network.message.BackpackItemCountPacket;
import sophisticated.building.network.message.BuildingUpgradeStatePacket;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
public class CommonEvents {

	private static final Map<UUID, Map<Item, Integer>> LAST_BACKPACK_COUNTS = new HashMap<>();
	private static final Map<UUID, int[]> LAST_UPGRADE_STATE = new HashMap<>();
	private static final Map<UUID, List<String>> LAST_BACKPACK_TOOLS = new HashMap<>();

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
			PacketDistributor.sendToPlayer(player, new BuildingUpgradeStatePacket(tier, maxBlocks));
		} catch (LinkageError ignored) {
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
			List<BreakToolHelper.ToolSlot> slots = ToolSwapperIntegration.collectBackpackTools(player);
			List<ItemStack> tools = new java.util.ArrayList<>(slots.size());
			List<String> fingerprint = new java.util.ArrayList<>(slots.size());
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
			PacketDistributor.sendToPlayer(player, new BackpackToolsPacket(tools));
		} catch (Exception | LinkageError e) {
			SophisticatedBuilding.logger.debug("Error syncing backpack tools: {}", e.getMessage());
		}
	}



	//Mod Bus Events
//	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	public static class ModBusEvents {


	}

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		PowerLevelCommand.register(event.getDispatcher());
	}



	@SubscribeEvent
	public static void onTick(LevelTickEvent.Pre event) {
		Level level = event.getLevel();
		if (level.isClientSide) return;
		if (!level.dimension().equals(Level.OVERWORLD)) return;

		SophisticatedBuilding.SERVER_BLOCK_PLACER.tick();
	}

	//Cancel event if necessary. Nothing more, rest is handled on mouseclick
	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (event.getLevel().isClientSide()) return; //Never called clientside anyway, but just to be sure
		if (!(event.getEntity() instanceof Player player)) return;
		if (event.getEntity() instanceof FakePlayer) return;

		//Don't cancel event if our custom logic is breaking blocks
		if (SophisticatedBuilding.SERVER_BLOCK_PLACER.isPlacingOrBreakingBlocks()) return;

		if (!ServerBuildState.isLikeVanilla(player)) {

			//Only cancel if itemblock in hand
			//Fixed issue with e.g. Create Wrench shift-rightclick disassembling being cancelled.
			if (isPlayerHoldingBlock(player)) {
				event.setCanceled(true);
				//Notify client to not decrease itemstack
				if (player instanceof ServerPlayer serverPlayer) {
					int slotIndex = 36 + serverPlayer.getInventory().selected;
					serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
							serverPlayer.inventoryMenu.containerId,
							serverPlayer.inventoryMenu.incrementStateId(),
							slotIndex,
							serverPlayer.getInventory().getSelected()
					));
				}
			}
		}
	}

	//Cancel event if necessary. Nothing more, rest is handled on mouseclick
	@SubscribeEvent
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (event.getLevel().isClientSide()) return;
		Player player = event.getPlayer();
		if (player instanceof FakePlayer) return;

		//Don't cancel event if our custom logic is breaking blocks
		if (SophisticatedBuilding.SERVER_BLOCK_PLACER.isPlacingOrBreakingBlocks()) return;

		// canBreakFar() now also returns true for survival players when survival breaking is
		// enabled (see PowerLevel.canBreakFar / ServerConfig.survivalBreaking), so vanilla mining
		// is cancelled here for survival players with a build mode active too, exactly like it
		// already was for creative - build-mode breaking replaces vanilla mining while a build
		// mode is on; switching to Disable mode restores vanilla (design decision D5).
		PowerLevel powerLevel = player.getData(SophisticatedBuilding.POWER_LEVEL);
		if (!ServerBuildState.isLikeVanilla(player) && powerLevel.canBreakFar(player)) {
			event.setCanceled(true);
		}
	}

	private static boolean isPlayerHoldingBlock(Player player) {
		ItemStack currentItemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
		return currentItemStack.getItem() instanceof BlockItem ||
				(CompatHelper.isItemBlockProxy(currentItemStack) && !player.isShiftKeyDown());
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		if (player.getCommandSenderWorld().isClientSide) return;

		ServerBuildState.handleNewPlayer(player);

		((ServerPlayer)player).connection.send(new ModifierSettingsPacket(player));

		PowerLevel powerLevel = player.getData(SophisticatedBuilding.POWER_LEVEL);
		((ServerPlayer)player).connection.send(new PowerLevelPacket(powerLevel.getPowerLevel()));

		sendBuildingUpgradeState((ServerPlayer) player, true);
		sendBackpackTools((ServerPlayer) player, true);
	}

	@SubscribeEvent
	public static void onClone(PlayerEvent.Clone event) {
		Player original = event.getOriginal();
		Player clone = event.getEntity();

		// Copy the power level from the original player to the clone
		// We want to copy on both death (if we want persistence) and dimension change
		if (original.hasData(SophisticatedBuilding.POWER_LEVEL)) {
			clone.setData(SophisticatedBuilding.POWER_LEVEL, original.getData(SophisticatedBuilding.POWER_LEVEL));
		}
	}



	@SubscribeEvent
	public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
		if (event.getEntity().level().isClientSide()) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (event.getSlot() != net.minecraft.world.entity.EquipmentSlot.MAINHAND) return;

		// Only sync backpack item counts if SophisticatedBackpacks is loaded
		if (!CompatHelper.isSophisticatedBackpacksLoaded()) return;

		try {
			ItemStack to = event.getTo();
			if (to.getItem() instanceof BlockItem) {
				BuildingUpgradeHelper.syncItemCount(player, to.getItem());
			} else if (to.getItem() instanceof sophisticated.building.item.AbstractRandomizerBagItem bagItem) {
				IItemHandler bagInventory = bagItem.getBagInventory(to);
				if (bagInventory != null) {
					List<ItemStack> templates = bagItem.getTemplates(bagInventory);
					for (ItemStack template : templates) {
						BuildingUpgradeHelper.syncItemCount(player, template.getItem());
					}
				}
			}
		} catch (LinkageError ignored) {
			// SophisticatedCore not available
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (!(player instanceof ServerPlayer serverPlayer)) return;
		if (player instanceof FakePlayer) return;
		if (serverPlayer.level().isClientSide) return;

		// Only sync backpack item counts if SophisticatedBackpacks is loaded
		if (!CompatHelper.isSophisticatedBackpacksLoaded()) return;

		// Only sync periodically to keep network traffic low
		if (serverPlayer.tickCount % 10 != 0) {
			return;
		}

		try {
			Set<Item> itemsToSync = new LinkedHashSet<>();
			ItemStack held = serverPlayer.getMainHandItem();

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

			Map<Item, Integer> lastCounts = LAST_BACKPACK_COUNTS.computeIfAbsent(serverPlayer.getUUID(), k -> new HashMap<>());
			for (Item item : itemsToSync) {
				// Unclamped total across every backpack with an enabled upgrade (RC2/T4).
				int count = BuildingUpgradeHelper.countBlockInBackpacksForDisplay(serverPlayer, new ItemStack(item));
				Integer last = lastCounts.get(item);
				if (last == null || last != count) {
					lastCounts.put(item, count);
					PacketDistributor.sendToPlayer(serverPlayer, new BackpackItemCountPacket(BuiltInRegistries.ITEM.getKey(item), count));
				}
			}

			// Drop stale entries to keep the cache compact
			lastCounts.keySet().removeIf(item -> !itemsToSync.contains(item));

			sendBuildingUpgradeState(serverPlayer, false);
			sendBackpackTools(serverPlayer, false);
		} catch (LinkageError ignored) {
			// SophisticatedCore not available
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		if (player.getCommandSenderWorld().isClientSide) {
			SophisticatedBuilding.log("PlayerLoggedOutEvent triggers on client side");
			return;
		}

		SophisticatedBuilding.UNDO_REDO.clear(player);
		LAST_BACKPACK_COUNTS.remove(player.getUUID());
		LAST_UPGRADE_STATE.remove(player.getUUID());
		LAST_BACKPACK_TOOLS.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		if (player.getCommandSenderWorld().isClientSide) {
			SophisticatedBuilding.log("PlayerRespawnEvent triggers on client side");
			return;
		}

		//TODO check if this is needed
		ServerBuildState.handleNewPlayer(player);

		if (player instanceof ServerPlayer serverPlayer) {
			LAST_UPGRADE_STATE.remove(serverPlayer.getUUID());
			LAST_BACKPACK_TOOLS.remove(serverPlayer.getUUID());
			sendBuildingUpgradeState(serverPlayer, true);
			sendBackpackTools(serverPlayer, true);
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		if (player.getCommandSenderWorld().isClientSide) {
			SophisticatedBuilding.log("PlayerChangedDimensionEvent triggers on client side");
			return;
		}

		//Undo redo has no dimension data, so clear it
		SophisticatedBuilding.UNDO_REDO.clear(player);

		// Sync power level to client when changing dimensions
		if (player instanceof ServerPlayer serverPlayer) {
			PowerLevel powerLevel = player.getData(SophisticatedBuilding.POWER_LEVEL);
			if (powerLevel != null) {
				serverPlayer.connection.send(new PowerLevelPacket(powerLevel.getPowerLevel()));
			}

			sendBuildingUpgradeState(serverPlayer, true);
			sendBackpackTools(serverPlayer, true);
		}

		//TODO disable build mode and modifiers?
	}
}
