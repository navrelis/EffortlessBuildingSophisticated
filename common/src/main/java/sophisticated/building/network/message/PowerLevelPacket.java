package sophisticated.building.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.network.ModPayload;

/**
 * Sync power level from server to client
 */
public final class PowerLevelPacket implements ModPayload {
	public static final ResourceLocation ID = SophisticatedBuilding.asResource("power_level");

	private final int powerLevel;

	public PowerLevelPacket(int powerLevel) {
		this.powerLevel = powerLevel;
	}

	public int powerLevel() {
		return powerLevel;
	}

	public PowerLevelPacket(FriendlyByteBuf buf) {
		this(buf.readInt());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeInt(powerLevel);
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static class Handler {
		public static void handle(final PowerLevelPacket packet, final Player player) {
			if (player != null) {
				PowerLevel currentLevel = AttachmentHandler.getOrCreatePowerLevel(player);
				currentLevel.setPowerLevel(packet.powerLevel);
				AttachmentHandler.setPowerLevel(player, currentLevel);
			}
		}
	}
}
