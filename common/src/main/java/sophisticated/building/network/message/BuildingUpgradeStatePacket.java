package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.client.ClientBuildingUpgradeState;
import sophisticated.building.network.ModPayload;

/**
 * Sync the player's best (highest-tier) Building Upgrade tier and effective max-blocks limit from
 * server to client. The client never inspects a backpack's upgrade inventory itself (see
 * 03_ROOT_CAUSE_BUILDING_UPGRADE.md RC2); it only ever uses the last value synced through this
 * packet, via {@link ClientBuildingUpgradeState}.
 */
public final class BuildingUpgradeStatePacket implements ModPayload {
    public static final ResourceLocation ID = SophisticatedBuilding.asResource("building_upgrade_state");

    private final int tier;
    private final int maxBlocks;

    public BuildingUpgradeStatePacket(int tier, int maxBlocks) {
        this.tier = tier;
        this.maxBlocks = maxBlocks;
    }

    public int tier() {
        return tier;
    }

    public int maxBlocks() {
        return maxBlocks;
    }

    public BuildingUpgradeStatePacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(tier);
        buf.writeInt(maxBlocks);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static class Handler {
        public static void handle(final BuildingUpgradeStatePacket packet, final Player player) {
            ClientBuildingUpgradeState.set(packet.tier(), packet.maxBlocks());
        }
    }
}
