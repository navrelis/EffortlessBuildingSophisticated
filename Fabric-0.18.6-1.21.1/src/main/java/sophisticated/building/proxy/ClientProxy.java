package sophisticated.building.proxy;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import sophisticated.building.SophisticatedBuilding;

@Environment(EnvType.CLIENT)
public class ClientProxy {

	public static void logTranslate(Player player, String prefix, String translationKey, String suffix, boolean actionBar) {
		SophisticatedBuilding.log(player, prefix + I18n.get(translationKey) + suffix, actionBar);
	}
}

