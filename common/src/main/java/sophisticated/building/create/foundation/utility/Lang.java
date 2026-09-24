package sophisticated.building.create.foundation.utility;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import sophisticated.building.SophisticatedBuilding;

public class Lang {

	public static MutableComponent translateDirect(String key, Object... args) {
		return new TranslatableComponent(SophisticatedBuilding.MODID + "." + key, args);
	}

	public static MutableComponent translate(String key, Object... args) {
		return new TranslatableComponent(key, args);
	}
}
