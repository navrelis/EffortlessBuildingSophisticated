package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.network.ModPayload;

/**
 * Send packet to client to translate and log the containing message
 */
public final class TranslatedLogPacket implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("translated_log");

	private final String prefix;
	private final String translationKey;
	private final String suffix;
	private final boolean actionBar;

	public TranslatedLogPacket(String prefix, String translationKey, String suffix, boolean actionBar) {
		this.prefix = prefix;
		this.translationKey = translationKey;
		this.suffix = suffix;
		this.actionBar = actionBar;
	}

	public String prefix() {
		return prefix;
	}

	public String translationKey() {
		return translationKey;
	}

	public String suffix() {
		return suffix;
	}

	public boolean actionBar() {
		return actionBar;
	}

	public TranslatedLogPacket(FriendlyByteBuf buf) {
		this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(prefix);
		buf.writeUtf(translationKey);
		buf.writeUtf(suffix);
		buf.writeBoolean(actionBar);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final TranslatedLogPacket packet, final Player player) {
			if (player != null) {
				SophisticatedBuilding.logTranslate(player, packet.prefix(), packet.translationKey(), packet.suffix(), packet.actionBar());
			}
		}
	}
}
