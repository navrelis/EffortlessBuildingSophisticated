package sophisticated.building.create.foundation.utility;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public final class Components {
	private static final Component IMMUTABLE_EMPTY = new TextComponent("");

	public static Component immutableEmpty() {
		return IMMUTABLE_EMPTY;
	}

	/** Use {@link #immutableEmpty()} when possible to prevent creating an extra object. */
	public static MutableComponent empty() {
		return new TextComponent("");
	}

	public static MutableComponent literal(String str) {
		return new TextComponent(str);
	}

	public static MutableComponent translatable(String key) {
		return new TranslatableComponent(key);
	}

	public static MutableComponent translatable(String key, Object... args) {
		return new TranslatableComponent(key, args);
	}

	public static MutableComponent keybind(String name) {
		return new KeybindComponent(name);
	}
}
