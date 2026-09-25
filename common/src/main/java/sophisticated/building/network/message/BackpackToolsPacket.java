package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBackpackToolCache;
import sophisticated.building.network.ModPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Syncs the tools currently found by {@code ToolSwapperIntegration.collectBackpackTools} (an
 * enabled Tool Swapper / Advanced Tool Swapper upgrade in one of the player's backpacks) from
 * server to client, for the survival-breaking preview/HUD. The client cannot reliably read a
 * backpack's upgrade inventory itself (see 08_SURVIVAL_BREAKING_ANALYSIS.md D6), so this is the
 * only source of backpack tool candidates on the client, via {@link ClientBackpackToolCache}.
 */
public final class BackpackToolsPacket implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("backpack_tools");

	private final List<ItemStack> tools;

	public BackpackToolsPacket(List<ItemStack> tools) {
		this.tools = tools;
	}

	public List<ItemStack> tools() {
		return tools;
	}

	public BackpackToolsPacket(FriendlyByteBuf buf) {
		this(readTools(buf));
	}

	private static List<ItemStack> readTools(FriendlyByteBuf buf) {
		int count = buf.readVarInt();
		List<ItemStack> tools = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			tools.add(buf.readItem());
		}
		return tools;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		// FriendlyByteBuf.writeCollection / readList of Minecraft 1.17+: a var-int count, then the stacks
		buf.writeVarInt(tools.size());
		for (ItemStack tool : tools) {
			buf.writeItem(tool);
		}
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
