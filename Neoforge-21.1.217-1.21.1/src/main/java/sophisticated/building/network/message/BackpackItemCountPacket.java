package sophisticated.building.network.message;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBackpackItemCache;

/**
 * Sync a single item count from a backpack to the client for HUD display.
 */
public record BackpackItemCountPacket(ResourceLocation itemId, int count) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, BackpackItemCountPacket> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            BackpackItemCountPacket::itemId,
            ByteBufCodecs.INT,
            BackpackItemCountPacket::count,
            BackpackItemCountPacket::new);

    public static final Type<BackpackItemCountPacket> ID = new Type<>(SophisticatedBuilding.asResource("backpack_item_count"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static class Handler {
        public static void handle(final BackpackItemCountPacket packet, final IPayloadContext context) {
            context.enqueueWork(() -> {
                Item item = BuiltInRegistries.ITEM.get(packet.itemId());
                if (item != null) {
                    ClientBackpackItemCache.setCount(item, packet.count());
                }
            });
        }
    }
}
