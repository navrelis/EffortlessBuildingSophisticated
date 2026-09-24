package sophisticated.building.network.message;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.network.ModPayload;
import sophisticated.building.platform.Services;

/**
 * Sync build modifiers between server and client, for saving and loading.
 */
public record ModifierSettingsPacket(CompoundTag modifiersTag) implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("modifier_settings");
	// Key of the modifier settings in the per-player data (see IPlatformHelper.getPersistentData)
	private static final String DATA_KEY = SophisticatedBuilding.MODID + ":buildModifiers";

	public ModifierSettingsPacket(FriendlyByteBuf buf) {
		this(buf.readNbt());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeNbt(modifiersTag);
	}

	public ModifierSettingsPacket(Player player) {
		this(player != null ? Services.PLATFORM.getPersistentData(player).getCompound(DATA_KEY) : new CompoundTag());
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class ServerHandler {
		public static void handleServer(final ModifierSettingsPacket packet, final Player sender) {
			if (sender instanceof ServerPlayer player) {
				Services.PLATFORM.getPersistentData(player).put(DATA_KEY, packet.modifiersTag().copy());
			}
		}
	}

	public static class ClientHandler {
		public static void handleClient(final ModifierSettingsPacket packet, final Player player) {
			SophisticatedBuildingClient.BUILD_MODIFIERS.deserializeNBT(packet.modifiersTag());
		}
	}
}
