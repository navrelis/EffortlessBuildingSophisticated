package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.item.OmegaRandomizerBagItem;

/**
 * Packet to update slot weight in Omega Randomizer Bag
 */
public record OmegaBagWeightPacket(int slotIndex, int weight) implements CustomPacketPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("omega_bag_weight");

	public OmegaBagWeightPacket(FriendlyByteBuf buf) {
		this(buf.readInt(), buf.readInt());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeInt(slotIndex);
		buf.writeInt(weight);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final OmegaBagWeightPacket packet, final Player player) {
			if (player != null) {
				ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
				if (heldItem.getItem() instanceof OmegaRandomizerBagItem omegaBag) {
					if (packet.slotIndex < 0 || packet.slotIndex >= OmegaRandomizerBagItem.INV_SIZE) {
						return;
					}
					omegaBag.setSlotWeight(heldItem, packet.slotIndex, packet.weight);
				}
			}
		}
	}
}
