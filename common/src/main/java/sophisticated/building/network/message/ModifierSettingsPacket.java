package sophisticated.building.network.message;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.SophisticatedBuildingClient;
import sophisticated.building.platform.Services;
import sophisticated.building.utilities.ModifierLimits;

/**
 * Sync build modifiers between server and client, for saving and loading.
 */
public record ModifierSettingsPacket(CompoundTag modifiersTag) implements CustomPacketPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("modifier_settings");
	// Key of the modifier settings in the per-player data (see IPlatformHelper.getPersistentData)
	public static final String DATA_KEY = SophisticatedBuilding.MODID + ":buildModifiers";

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
				//Stored capped to the player's limits (arrays within the blocks per axis, mirrors within the radius), as the
				//client builds with them; the server's request checks rely on these settings
				CompoundTag capped = ModifierLimits.cap(packet.modifiersTag().copy(), AttachmentHandler.getMaxBlocksPerAxis(player, false),
						AttachmentHandler.getMaxMirrorRadius(player, false));
				Services.PLATFORM.getPersistentData(player).put(DATA_KEY, capped);
			}
		}
	}

	public static class ClientHandler {
		public static void handleClient(final ModifierSettingsPacket packet, final Player player) {
			SophisticatedBuildingClient.BUILD_MODIFIERS.deserializeNBT(packet.modifiersTag());
		}
	}
}
