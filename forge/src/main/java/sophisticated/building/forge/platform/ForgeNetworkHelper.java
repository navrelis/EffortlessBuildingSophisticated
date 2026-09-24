package sophisticated.building.forge.platform;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import sophisticated.building.forge.ForgeNetworking;
import sophisticated.building.network.ModPayload;
import sophisticated.building.platform.services.INetworkHelper;

public final class ForgeNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(ModPayload payload) {
        ForgeNetworking.channel().sendToServer(new ForgeNetworking.Message(payload));
    }

    @Override
    public void sendToPlayer(ServerPlayer player, ModPayload payload) {
        ForgeNetworking.channel().send(PacketDistributor.PLAYER.with(() -> player), new ForgeNetworking.Message(payload));
    }
}
