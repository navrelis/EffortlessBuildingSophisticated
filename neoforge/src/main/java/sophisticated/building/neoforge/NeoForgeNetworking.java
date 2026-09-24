package sophisticated.building.neoforge;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.PacketHandler;

import java.util.Optional;

/**
 * Registers the payloads of {@link PacketHandler}: types in both lists as bidirectional. The
 * handlers run on the main thread with the context player; when one throws, the connection is closed
 * with the payload's failure message (if it has one).
 */
public final class NeoForgeNetworking {

	private NeoForgeNetworking() {
	}

	public static void setupPackets(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(SophisticatedBuilding.MODID);

		for (PacketHandler.Payload<?> serverbound : PacketHandler.SERVERBOUND) {
			Optional<PacketHandler.Payload<?>> clientbound = find(PacketHandler.CLIENTBOUND, serverbound.type());
			if (clientbound.isPresent()) {
				registerBidirectional(registrar, serverbound, clientbound.get());
			} else {
				registerToServer(registrar, serverbound);
			}
		}
		for (PacketHandler.Payload<?> clientbound : PacketHandler.CLIENTBOUND) {
			if (find(PacketHandler.SERVERBOUND, clientbound.type()).isEmpty()) {
				registerToClient(registrar, clientbound);
			}
		}
	}

	private static Optional<PacketHandler.Payload<?>> find(Iterable<PacketHandler.Payload<?>> payloads, CustomPacketPayload.Type<?> type) {
		for (PacketHandler.Payload<?> payload : payloads) {
			if (payload.type().equals(type)) {
				return Optional.of(payload);
			}
		}
		return Optional.empty();
	}

	private static <T extends CustomPacketPayload> void registerToServer(PayloadRegistrar registrar, PacketHandler.Payload<T> payload) {
		registrar.playToServer(payload.type(), payload.codec(), handler(payload));
	}

	private static <T extends CustomPacketPayload> void registerToClient(PayloadRegistrar registrar, PacketHandler.Payload<T> payload) {
		registrar.playToClient(payload.type(), payload.codec(), handler(payload));
	}

	@SuppressWarnings("unchecked")
	private static <T extends CustomPacketPayload> void registerBidirectional(PayloadRegistrar registrar, PacketHandler.Payload<T> serverbound, PacketHandler.Payload<?> clientbound) {
		PacketHandler.Payload<T> toClient = (PacketHandler.Payload<T>) clientbound;
		registrar.playBidirectional(serverbound.type(), serverbound.codec(), new DirectionalPayloadHandler<>(handler(toClient), handler(serverbound)));
	}

	private static <T extends CustomPacketPayload> IPayloadHandler<T> handler(PacketHandler.Payload<T> payload) {
		return (packet, context) -> context.enqueueWork(() -> payload.handler().accept(packet, context.player()))
				.exceptionally(e -> {
					if (payload.failureKey() != null) {
						context.disconnect(Component.translatable("sophisticatedbuilding.networking." + payload.failureKey() + ".failed", e.getMessage()));
					}
					return null;
				});
	}
}
