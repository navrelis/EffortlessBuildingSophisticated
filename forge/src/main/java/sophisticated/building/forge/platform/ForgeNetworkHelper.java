package sophisticated.building.forge.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import sophisticated.building.forge.ForgeNetworking;
import sophisticated.building.platform.services.INetworkHelper;

public final class ForgeNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ForgeNetworking.send(payload, PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ForgeNetworking.send(payload, PacketDistributor.PLAYER.with(player));
    }
}
