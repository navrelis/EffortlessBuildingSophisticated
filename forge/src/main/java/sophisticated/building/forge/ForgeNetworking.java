package sophisticated.building.forge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.payload.PayloadFlow;
import net.minecraftforge.network.payload.PayloadProtocol;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.PacketHandler;

import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Registers the payloads of {@link PacketHandler} on one Forge payload channel (each payload keeps its
 * own type id on the wire): types in both lists as bidirectional. The handlers run on the main thread
 * with the context player; when one throws, the exception is logged and the connection is closed with
 * the payload's failure message (if it has one), as on NeoForge.
 */
public final class ForgeNetworking {

	private static final int PROTOCOL_VERSION = 1;

	private static Channel<CustomPacketPayload> channel;

	private ForgeNetworking() {
	}

	/** The mod's payload channel, see {@link sophisticated.building.forge.platform.ForgeNetworkHelper}. */
	public static Channel<CustomPacketPayload> channel() {
		return channel;
	}

	public static void setupPackets() {
		PayloadProtocol<RegistryFriendlyByteBuf, CustomPacketPayload> play = ChannelBuilder.named(SophisticatedBuilding.asResource("main"))
				.networkProtocolVersion(PROTOCOL_VERSION)
				.payloadChannel()
				.play();
		PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> toServer = play.serverbound();
		PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> toClient = play.clientbound();
		PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> bidirectional = play.bidirectional();

		for (PacketHandler.Payload<?> serverbound : PacketHandler.SERVERBOUND) {
			Optional<PacketHandler.Payload<?>> clientbound = find(PacketHandler.CLIENTBOUND, serverbound.type());
			if (clientbound.isPresent()) {
				registerBidirectional(bidirectional, serverbound, clientbound.get());
			} else {
				register(toServer, serverbound);
			}
		}
		for (PacketHandler.Payload<?> clientbound : PacketHandler.CLIENTBOUND) {
			if (find(PacketHandler.SERVERBOUND, clientbound.type()).isEmpty()) {
				register(toClient, clientbound);
			}
		}

		// The flows share one builder: building any of them builds the channel with every payload.
		channel = bidirectional.build();
	}

	private static Optional<PacketHandler.Payload<?>> find(Iterable<PacketHandler.Payload<?>> payloads, CustomPacketPayload.Type<?> type) {
		for (PacketHandler.Payload<?> payload : payloads) {
			if (payload.type().equals(type)) {
				return Optional.of(payload);
			}
		}
		return Optional.empty();
	}

	private static <T extends CustomPacketPayload> void register(PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, PacketHandler.Payload<T> payload) {
		flow.add(payload.type(), codec(payload), handler(payload));
	}

	/** One registration per type: the handler of the receiving side's direction runs. */
	@SuppressWarnings("unchecked")
	private static <T extends CustomPacketPayload> void registerBidirectional(PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, PacketHandler.Payload<T> serverbound, PacketHandler.Payload<?> clientbound) {
		BiConsumer<T, CustomPayloadEvent.Context> toServer = handler(serverbound);
		BiConsumer<T, CustomPayloadEvent.Context> toClient = handler((PacketHandler.Payload<T>) clientbound);
		flow.add(serverbound.type(), codec(serverbound), (packet, context) -> {
			if (context.isServerSide()) {
				toServer.accept(packet, context);
			} else {
				toClient.accept(packet, context);
			}
		});
	}

	// The codecs read any RegistryFriendlyByteBuf subtype, Forge's flow wants the exact buffer type.
	@SuppressWarnings("unchecked")
	private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(PacketHandler.Payload<T> payload) {
		return (StreamCodec<RegistryFriendlyByteBuf, T>) payload.codec();
	}

	private static <T extends CustomPacketPayload> BiConsumer<T, CustomPayloadEvent.Context> handler(PacketHandler.Payload<T> payload) {
		return (packet, context) -> {
			context.enqueueWork(() -> {
				Player player = context.isServerSide() ? context.getSender() : SophisticatedBuildingForgeClient.localPlayer();
				try {
					payload.handler().accept(packet, player);
				} catch (RuntimeException e) {
					// Logged like NeoForge logs a failed payload task, not rethrown
					SophisticatedBuilding.logger.error("Failed to process a synchronized task of the payload: {}", payload.type().id(), e);
					if (payload.failureKey() != null) {
						context.getConnection().disconnect(Component.translatable("sophisticatedbuilding.networking." + payload.failureKey() + ".failed", e.toString()));
					}
				}
			});
			context.setPacketHandled(true);
		};
	}
}
