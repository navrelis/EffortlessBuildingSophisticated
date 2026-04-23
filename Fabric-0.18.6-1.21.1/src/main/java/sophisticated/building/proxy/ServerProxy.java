package sophisticated.building.proxy;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.network.message.TranslatedLogPacket;

public class ServerProxy {
	//Only physical server! Singleplayer server is seen as clientproxy

	public static void logTranslate(Player player, String prefix, String translationKey, String suffix, boolean actionBar) {
		if (player instanceof ServerPlayer serverPlayer) {
			ServerPlayNetworking.send(serverPlayer, new TranslatedLogPacket(prefix, translationKey, suffix, actionBar));
		}
	}
}
