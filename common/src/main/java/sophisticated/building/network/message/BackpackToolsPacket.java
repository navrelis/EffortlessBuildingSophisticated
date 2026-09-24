package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBackpackToolCache;
import sophisticated.building.network.ModPayload;

import java.util.List;

/**
 * Syncs the tools currently found by {@code ToolSwapperIntegration.collectBackpackTools} (an
 * enabled Tool Swapper / Advanced Tool Swapper upgrade in one of the player's backpacks) from
 * server to client, for the survival-breaking preview/HUD. The client cannot reliably read a
 * backpack's upgrade inventory itself (see 08_SURVIVAL_BREAKING_ANALYSIS.md D6), so this is the
 * only source of backpack tool candidates on the client, via {@link ClientBackpackToolCache}.
 */
public record BackpackToolsPacket(List<ItemStack> tools) implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("backpack_tools");

	public BackpackToolsPacket(FriendlyByteBuf buf) {
		this(buf.readList(FriendlyByteBuf::readItem));
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeCollection(tools, FriendlyByteBuf::writeItem);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final BackpackToolsPacket packet, final Player player) {
			ClientBackpackToolCache.set(packet.tools());
		}
	}
}
