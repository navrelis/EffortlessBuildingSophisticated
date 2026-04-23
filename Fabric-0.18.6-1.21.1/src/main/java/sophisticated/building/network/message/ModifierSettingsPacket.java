package sophisticated.building.network.message;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.SophisticatedBuildingClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sync build modifiers between server and client, for saving and loading.
 */
public record ModifierSettingsPacket(CompoundTag modifiersTag) implements CustomPacketPayload {
	public static final StreamCodec<FriendlyByteBuf, ModifierSettingsPacket> CODEC = StreamCodec.composite(
			ByteBufCodecs.COMPOUND_TAG,
			ModifierSettingsPacket::modifiersTag,
			ModifierSettingsPacket::new);
	public static final Type<ModifierSettingsPacket> ID = new Type<>(SophisticatedBuilding.asResource("modifier_settings"));
	private static final Map<UUID, CompoundTag> PLAYER_MODIFIERS = new ConcurrentHashMap<>();

	public ModifierSettingsPacket(Player player) {
		this(player != null ? PLAYER_MODIFIERS.getOrDefault(player.getUUID(), new CompoundTag()) : new CompoundTag());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ID;
	}

	public static class ServerHandler {
		public static void handleServer(final ModifierSettingsPacket packet, final ServerPlayNetworking.Context context) {
			context.server().execute(() -> {
				if (context.player() instanceof ServerPlayer player) {
					PLAYER_MODIFIERS.put(player.getUUID(), packet.modifiersTag().copy());
				}
			});
		}
	}

	public static class ClientHandler {
		public static void handleClient(final ModifierSettingsPacket packet, final ClientPlayNetworking.Context context) {
			context.client().execute(() -> SophisticatedBuildingClient.BUILD_MODIFIERS.deserializeNBT(packet.modifiersTag()));
		}
	}
}
