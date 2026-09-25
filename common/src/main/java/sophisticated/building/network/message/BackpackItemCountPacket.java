package sophisticated.building.network.message;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBackpackItemCache;
import sophisticated.building.network.ModPayload;

/**
 * Sync a single item count from a backpack to the client for HUD display.
 */
public record BackpackItemCountPacket(ResourceLocation itemId, int count) implements ModPayload {
    public static final ResourceLocation ID = SophisticatedBuilding.asResource("backpack_item_count");

    public BackpackItemCountPacket(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(itemId);
        buf.writeInt(count);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static class Handler {
        public static void handle(final BackpackItemCountPacket packet, final Player player) {
            Item item = BuiltInRegistries.ITEM.getOptional(packet.itemId()).orElse(null);
            if (item != null) {
                ClientBackpackItemCache.setCount(item, Math.max(0, packet.count()));
            }
        }
    }
}
