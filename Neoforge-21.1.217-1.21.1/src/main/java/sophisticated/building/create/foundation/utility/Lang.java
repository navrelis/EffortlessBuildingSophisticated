package sophisticated.building.create.foundation.utility;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import sophisticated.building.SophisticatedBuilding;

public class Lang {

	public static MutableComponent translateDirect(String key, Object... args) {
		return Component.translatable(SophisticatedBuilding.MODID + "." + key, args);
	}

	public static MutableComponent translate(String key, Object... args) {
		return Component.translatable(key, args);
	}
}
