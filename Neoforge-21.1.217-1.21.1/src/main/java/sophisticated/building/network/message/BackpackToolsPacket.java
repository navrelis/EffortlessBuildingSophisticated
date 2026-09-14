package sophisticated.building.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBackpackToolCache;

import java.util.List;

/**
 * Syncs the tools currently found by {@code ToolSwapperIntegration.collectBackpackTools} (an
 * enabled Tool Swapper / Advanced Tool Swapper upgrade in one of the player's backpacks) from
 * server to client, for the survival-breaking preview/HUD. The client cannot reliably read a
 * backpack's upgrade inventory itself (see 08_SURVIVAL_BREAKING_ANALYSIS.md D6), so this is the
 * only source of backpack tool candidates on the client, via {@link ClientBackpackToolCache}.
 */
public record BackpackToolsPacket(List<ItemStack> tools) implements CustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, BackpackToolsPacket> CODEC =
			ItemStack.OPTIONAL_LIST_STREAM_CODEC.map(BackpackToolsPacket::new, BackpackToolsPacket::tools);

	public static final Type<BackpackToolsPacket> ID = new Type<>(SophisticatedBuilding.asResource("backpack_tools"));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class Handler {
		public static void handle(final BackpackToolsPacket packet, final IPayloadContext context) {
			context.enqueueWork(() -> ClientBackpackToolCache.set(packet.tools()))
					.exceptionally(e -> {
						context.disconnect(Component.translatable("sophisticatedbuilding.networking.backpack_tools.failed", e.getMessage()));
						return null;
					});
		}
	}
}
