package sophisticated.building.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;
import sophisticated.building.network.PacketHandler;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Sends the payloads of {@link PacketHandler} over one Forge {@link SimpleChannel}. Forge 1.18.2 identifies the
 * messages of a channel by their class, so every payload travels in one {@link Message}, which writes the payload's
 * id before its body; the receiving side reads it with the payload's reader and runs the handler of its direction
 * (a payload type in both lists is bidirectional). The handlers run on the main thread with the context player; when
 * one throws, the exception is logged and the connection is closed with the payload's failure message (if it has
 * one), as on the other loaders.
 */
public final class ForgeNetworking {

	private static final String PROTOCOL_VERSION = "1";

	private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(SophisticatedBuilding.asResource("main"),
			() -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

	private ForgeNetworking() {
	}

	/** The mod's channel, see {@link sophisticated.building.forge.platform.ForgeNetworkHelper}. */
	public static SimpleChannel channel() {
		return CHANNEL;
	}

	public static void setupPackets() {
		CHANNEL.messageBuilder(Message.class, 0)
				.encoder(Message::write)
				.decoder(Message::read)
				.consumer(ForgeNetworking::handle)
				.add();
	}

	private static Optional<PacketHandler.Payload<?>> find(Iterable<PacketHandler.Payload<?>> payloads, ResourceLocation id) {
		for (PacketHandler.Payload<?> payload : payloads) {
			if (payload.id().equals(id)) {
				return Optional.of(payload);
			}
		}
		return Optional.empty();
	}

	private static void handle(Message message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		boolean serverSide = context.getDirection().getReceptionSide().isServer();
		// Forge 1.18.2 has no consumerMainThread: the handler moves itself to the main thread
		context.enqueueWork(() -> {
			// Only the handler of the receiving side's direction runs; a payload sent the wrong way is dropped
			find(serverSide ? PacketHandler.SERVERBOUND : PacketHandler.CLIENTBOUND, message.payload().id()).ifPresent(payload -> {
				Player player = serverSide ? context.getSender() : SophisticatedBuildingForgeClient.localPlayer();
				try {
					accept(payload, message.payload(), player);
				} catch (RuntimeException e) {
					// Logged like NeoForge logs a failed payload task, not rethrown
					SophisticatedBuilding.logger.error("Failed to process a synchronized task of the payload: {}", payload.id(), e);
					if (payload.failureKey() != null) {
						context.getNetworkManager().disconnect(new TranslatableComponent("sophisticatedbuilding.networking." + payload.failureKey() + ".failed", e.toString()));
					}
				}
			});
		});
		context.setPacketHandled(true);
	}

	// The payload was read by this entry's reader (same id), so it has the entry's type
	@SuppressWarnings("unchecked")
	private static <T extends ModPayload> void accept(PacketHandler.Payload<T> payload, ModPayload packet, Player player) {
		payload.handler().accept((T) packet, player);
	}

	/** One payload on the wire: its id, then its body. */
	public static final class Message {
		private final ModPayload payload;

		public Message(ModPayload payload) {
			this.payload = payload;
		}

		public ModPayload payload() {
			return payload;
		}

		private void write(FriendlyByteBuf buf) {
			buf.writeResourceLocation(payload.id());
			payload.write(buf);
		}

		private static Message read(FriendlyByteBuf buf) {
			ResourceLocation id = buf.readResourceLocation();
			// A bidirectional payload has the same reader in both lists
			Optional<PacketHandler.Payload<?>> found = find(PacketHandler.SERVERBOUND, id);
			if (!found.isPresent()) {
				found = find(PacketHandler.CLIENTBOUND, id);
			}
			PacketHandler.Payload<?> payload = found.orElseThrow(() -> new IllegalArgumentException("Unknown payload " + id));
			return new Message(payload.reader().apply(buf));
		}
	}
}
