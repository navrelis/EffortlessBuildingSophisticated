package sophisticated.building.platform.services;

import net.minecraft.server.level.ServerPlayer;
import sophisticated.building.network.ModPayload;

/**
 * Sends the mod's payloads. Registration of the payload types and their handlers happens in the
 * loader projects, from the list in {@code sophisticated.building.network.PacketHandler}.
 */
public interface INetworkHelper {

    /** Client only. */
    void sendToServer(ModPayload payload);

    void sendToPlayer(ServerPlayer player, ModPayload payload);
}
