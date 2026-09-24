package sophisticated.building.forge;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.network.PacketDistributor;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.PacketHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends the payloads of {@link PacketHandler} on one Forge channel. Each message is the payload's type id followed by
 * the payload itself, so every payload keeps its own type id on the wire (as on 1.21.1); the receiving side looks the
 * id up in the list of its direction, so a type in both lists is bidirectional. The handlers run on the main thread
 * with the context player; when one throws, the exception is logged and the connection is closed with the payload's
 * failure message (if it has one), as on NeoForge.
 */
public final class ForgeNetworking {

	private static final int PROTOCOL_VERSION = 1;

	private static final Map<ResourceLocation, PacketHandler.Payload<?>> SERVERBOUND = byId(PacketHandler.SERVERBOUND);
	private static final Map<ResourceLocation, PacketHandler.Payload<?>> CLIENTBOUND = byId(PacketHandler.CLIENTBOUND);

	private static EventNetworkChannel channel;

	private ForgeNetworking() {
	}

	public static void setupPackets() {
		channel = ChannelBuilder.named(SophisticatedBuilding.asResource("main"))
				.networkProtocolVersion(PROTOCOL_VERSION)
				.eventNetworkChannel()
				.addListener(ForgeNetworking::receive);
	}

	public static void send(CustomPacketPayload payload, PacketDistributor.PacketTarget target) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeResourceLocation(payload.id());
		payload.write(buf);
		channel.send(buf, target);
	}

	private static Map<ResourceLocation, PacketHandler.Payload<?>> byId(Iterable<PacketHandler.Payload<?>> payloads) {
		Map<ResourceLocation, PacketHandler.Payload<?>> map = new HashMap<>();
		for (PacketHandler.Payload<?> payload : payloads) {
			map.put(payload.id(), payload);
		}
		return map;
	}

	private static void receive(CustomPayloadEvent event) {
		CustomPayloadEvent.Context context = event.getSource();
		FriendlyByteBuf buf = event.getPayload();
		if (buf == null || !buf.isReadable()) {
			return;
		}
		ResourceLocation id = buf.readResourceLocation();
		PacketHandler.Payload<?> payload = (context.isServerSide() ? SERVERBOUND : CLIENTBOUND).get(id);
		if (payload == null) {
			SophisticatedBuilding.logger.error("Received unknown payload {} on channel {}", id, event.getChannel());
			return;
		}
		handle(payload, buf, context);
		context.setPacketHandled(true);
	}

	private static <T extends CustomPacketPayload> void handle(PacketHandler.Payload<T> payload, FriendlyByteBuf buf, CustomPayloadEvent.Context context) {
		T packet = payload.reader().apply(buf);
		context.enqueueWork(() -> {
			Player player = context.isServerSide() ? context.getSender() : SophisticatedBuildingForgeClient.localPlayer();
			try {
				payload.handler().accept(packet, player);
			} catch (RuntimeException e) {
				// Logged like NeoForge logs a failed payload task, not rethrown
				SophisticatedBuilding.logger.error("Failed to process a synchronized task of the payload: {}", payload.id(), e);
				if (payload.failureKey() != null) {
					context.getConnection().disconnect(Component.translatable("sophisticatedbuilding.networking." + payload.failureKey() + ".failed", e.toString()));
				}
			}
		});
	}
}
