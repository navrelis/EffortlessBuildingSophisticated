package sophisticated.building.neoforge;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.PacketHandler;

import java.util.Optional;

/**
 * Registers the payloads of {@link PacketHandler}: types in both lists with a server and a client
 * handler. The handlers run on the main thread with the context player; when one throws, the
 * connection is closed with the payload's failure message (if it has one).
 */
public final class NeoForgeNetworking {

	private NeoForgeNetworking() {
	}

	public static void setupPackets(final RegisterPayloadHandlerEvent event) {
		final IPayloadRegistrar registrar = event.registrar(SophisticatedBuilding.MODID);

		for (PacketHandler.Payload<?> serverbound : PacketHandler.SERVERBOUND) {
			Optional<PacketHandler.Payload<?>> clientbound = find(PacketHandler.CLIENTBOUND, serverbound.id());
			if (clientbound.isPresent()) {
				registerBidirectional(registrar, serverbound, clientbound.get());
			} else {
				registerToServer(registrar, serverbound);
			}
		}
		for (PacketHandler.Payload<?> clientbound : PacketHandler.CLIENTBOUND) {
			if (find(PacketHandler.SERVERBOUND, clientbound.id()).isEmpty()) {
				registerToClient(registrar, clientbound);
			}
		}
	}

	private static Optional<PacketHandler.Payload<?>> find(Iterable<PacketHandler.Payload<?>> payloads, ResourceLocation id) {
		for (PacketHandler.Payload<?> payload : payloads) {
			if (payload.id().equals(id)) {
				return Optional.of(payload);
			}
		}
		return Optional.empty();
	}

	private static <T extends CustomPacketPayload> void registerToServer(IPayloadRegistrar registrar, PacketHandler.Payload<T> payload) {
		registrar.play(payload.id(), payload.reader(), handlers -> handlers.server(handler(payload)));
	}

	private static <T extends CustomPacketPayload> void registerToClient(IPayloadRegistrar registrar, PacketHandler.Payload<T> payload) {
		registrar.play(payload.id(), payload.reader(), handlers -> handlers.client(handler(payload)));
	}

	@SuppressWarnings("unchecked")
	private static <T extends CustomPacketPayload> void registerBidirectional(IPayloadRegistrar registrar, PacketHandler.Payload<T> serverbound, PacketHandler.Payload<?> clientbound) {
		PacketHandler.Payload<T> toClient = (PacketHandler.Payload<T>) clientbound;
		registrar.play(serverbound.id(), serverbound.reader(), handlers -> handlers.server(handler(serverbound)).client(handler(toClient)));
	}

	private static <T extends CustomPacketPayload> IPlayPayloadHandler<T> handler(PacketHandler.Payload<T> payload) {
		return (packet, context) -> context.workHandler().submitAsync(() -> payload.handler().accept(packet, context.player().orElse(null)))
				.exceptionally(e -> {
					if (payload.failureKey() != null) {
						context.packetHandler().disconnect(Component.translatable("sophisticatedbuilding.networking." + payload.failureKey() + ".failed", e.getMessage()));
					}
					return null;
				});
	}
}
