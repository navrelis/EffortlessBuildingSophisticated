package sophisticated.building.forge;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import sophisticated.building.CommonEvents;
import sophisticated.building.SophisticatedBuilding;

/**
 * Server game events, forwarded to the loader-neutral handlers. Forge has no fake player type (it
 * was removed in Forge 1.20.5), so unlike on NeoForge there is no fake player check.
 */
@Mod.EventBusSubscriber(modid = SophisticatedBuilding.MODID)
public class ForgeCommonEvents {

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		CommonEvents.registerCommands(event.getDispatcher());
	}

	@SubscribeEvent
	public static void onTick(TickEvent.LevelTickEvent.Pre event) {
		if (event.level instanceof ServerLevel level) {
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

		if (CommonEvents.shouldCancelBlockPlace(player)) {
			event.setCanceled(true);
			//Notify client to not decrease itemstack
			if (player instanceof ServerPlayer serverPlayer) {
				int slotIndex = 36 + serverPlayer.getInventory().getSelectedSlot();
				serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
						serverPlayer.inventoryMenu.containerId,
						serverPlayer.inventoryMenu.incrementStateId(),
						slotIndex,
						serverPlayer.getInventory().getSelectedItem()
				));
			}
		}
	}

	//Cancel event if necessary. Nothing more, rest is handled on mouseclick
	@SubscribeEvent
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (event.getLevel().isClientSide()) return;

		if (CommonEvents.shouldCancelBlockBreak(event.getPlayer())) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;

		CommonEvents.onPlayerLoggedIn(player);
	}

	@SubscribeEvent
	public static void onClone(PlayerEvent.Clone event) {
		Player original = event.getOriginal();
		Player clone = event.getEntity();

		// Copy the power level from the original player to the clone, on both death (whose capabilities
		// Forge has invalidated already) and return from the End
		original.reviveCaps();
		if (PowerLevelCapability.has(original)) {
			PowerLevelCapability.set(clone, PowerLevelCapability.get(original));
		}
		original.invalidateCaps();
	}

	@SubscribeEvent
	public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		if (event.getSlot() != EquipmentSlot.MAINHAND) return;

		CommonEvents.onMainHandChanged(player, event.getTo());
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
		if (!(event.player instanceof ServerPlayer player)) return;

		CommonEvents.onPlayerTick(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			SophisticatedBuilding.log("PlayerLoggedOutEvent triggers on client side");
			return;
		}

		CommonEvents.onPlayerLoggedOut(player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			SophisticatedBuilding.log("PlayerRespawnEvent triggers on client side");
			return;
		}

		CommonEvents.onPlayerRespawned(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			SophisticatedBuilding.log("PlayerChangedDimensionEvent triggers on client side");
			return;
		}

		CommonEvents.onPlayerChangedDimension(player);
	}
}
