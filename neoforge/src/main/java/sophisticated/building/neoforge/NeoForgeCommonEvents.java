package sophisticated.building.neoforge;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import sophisticated.building.CommonEvents;
import sophisticated.building.SophisticatedBuilding;

/**
 * Server game events, forwarded to the loader-neutral handlers.
 */
@EventBusSubscriber(modid = SophisticatedBuilding.MODID)
public class NeoForgeCommonEvents {

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		CommonEvents.registerCommands(event.getDispatcher());
	}

	@SubscribeEvent
	public static void onTick(LevelTickEvent.Pre event) {
		if (event.getLevel() instanceof ServerLevel level) {
			CommonEvents.onLevelTick(level);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		CommonEvents.onServerStopped();
	}

	//Cancel event if necessary. Nothing more, rest is handled on mouseclick
	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (event.getLevel().isClientSide()) return; //Never called clientside anyway, but just to be sure
		if (!(event.getEntity() instanceof Player player)) return;
		if (event.getEntity() instanceof FakePlayer) return;

		if (CommonEvents.shouldCancelBlockPlace(player)) {
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

	//Cancel event if necessary. Nothing more, rest is handled on mouseclick
	@SubscribeEvent
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (event.getLevel().isClientSide()) return;
		Player player = event.getPlayer();
		if (player instanceof FakePlayer) return;

		if (CommonEvents.shouldCancelBlockBreak(player)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;

		CommonEvents.onPlayerLoggedIn(player);
	}

	@SubscribeEvent
	public static void onClone(PlayerEvent.Clone event) {
		Player original = event.getOriginal();
		Player clone = event.getEntity();

		// Copy the power level from the original player to the clone
		// We want to copy on both death (if we want persistence) and dimension change
		if (original.hasData(NeoForgeAttachments.POWER_LEVEL)) {
			clone.setData(NeoForgeAttachments.POWER_LEVEL, original.getData(NeoForgeAttachments.POWER_LEVEL));
		}
	}

	@SubscribeEvent
	public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (event.getSlot() != EquipmentSlot.MAINHAND) return;

		CommonEvents.onMainHandChanged(player, event.getTo());
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (player instanceof FakePlayer) return;

		CommonEvents.onPlayerTick(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			SophisticatedBuilding.log("PlayerLoggedOutEvent triggers on client side");
			return;
		}

		CommonEvents.onPlayerLoggedOut(player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			SophisticatedBuilding.log("PlayerRespawnEvent triggers on client side");
			return;
		}

		CommonEvents.onPlayerRespawned(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			SophisticatedBuilding.log("PlayerChangedDimensionEvent triggers on client side");
			return;
		}

		CommonEvents.onPlayerChangedDimension(player);
	}
}
