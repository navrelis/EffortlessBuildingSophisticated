package sophisticated.building.network.message;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.item.OmegaRandomizerBagItem;

/**
 * Packet to update slot weight in Omega Randomizer Bag
 */
public record OmegaBagWeightPacket(int slotIndex, int weight) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, OmegaBagWeightPacket> CODEC = StreamCodec.composite(
			ByteBufCodecs.INT,
			OmegaBagWeightPacket::slotIndex,
			ByteBufCodecs.INT,
			OmegaBagWeightPacket::weight,
			OmegaBagWeightPacket::new);
	public static final Type<OmegaBagWeightPacket> ID = new Type<>(SophisticatedBuilding.asResource("omega_bag_weight"));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class Handler {
		public static void handle(final OmegaBagWeightPacket packet, final ServerPlayNetworking.Context context) {
			context.server().execute(() -> {
				Player player = context.player();
				if (player != null) {
					ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
					if (heldItem.getItem() instanceof OmegaRandomizerBagItem omegaBag) {
						if (packet.slotIndex < 0 || packet.slotIndex >= OmegaRandomizerBagItem.INV_SIZE) {
							return;
						}
						omegaBag.setSlotWeight(heldItem, packet.slotIndex, packet.weight);
					}
				}
			});
		}
	}
}
