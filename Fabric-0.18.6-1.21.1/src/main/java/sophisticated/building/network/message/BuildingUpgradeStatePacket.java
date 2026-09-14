package sophisticated.building.network.message;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBuildingUpgradeState;

/**
 * Sync the player's best (highest-tier) Building Upgrade tier and effective max-blocks limit from
 * server to client. The client never inspects a backpack's upgrade inventory itself (see
 * 03_ROOT_CAUSE_BUILDING_UPGRADE.md RC2); it only ever uses the last value synced through this
 * packet, via {@link ClientBuildingUpgradeState}.
 */
public record BuildingUpgradeStatePacket(int tier, int maxBlocks) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, BuildingUpgradeStatePacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            BuildingUpgradeStatePacket::tier,
            ByteBufCodecs.INT,
            BuildingUpgradeStatePacket::maxBlocks,
            BuildingUpgradeStatePacket::new);

    public static final Type<BuildingUpgradeStatePacket> ID = new Type<>(SophisticatedBuilding.asResource("building_upgrade_state"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static class Handler {
        public static void handle(final BuildingUpgradeStatePacket packet, final ClientPlayNetworking.Context context) {
            context.client().execute(() -> ClientBuildingUpgradeState.set(packet.tier(), packet.maxBlocks()));
        }
    }
}
