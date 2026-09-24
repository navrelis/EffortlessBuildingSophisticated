package sophisticated.building.create.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelAccessor;
import sophisticated.building.create.CreateClient;
import sophisticated.building.create.catnip.animation.AnimationTickHolder;
import sophisticated.building.create.catnip.outliner.Outliner;
import sophisticated.building.create.foundation.utility.CameraAngleAnimationService;

/**
 * Client hooks of the vendored Create/Catnip code, called by the loader projects: {@link #onTick()}
 * at the end of every client tick, the world hooks when the client world is loaded/unloaded.
 */
public class ClientEvents {

	public static void onTick() {
		if (!isGameActive()) return;

		AnimationTickHolder.tick();

		CreateClient.GHOST_BLOCKS.tickGhosts();
		Outliner.getInstance().tickOutlines();
		CameraAngleAnimationService.tick();
	}

	public static void onLoadWorld(LevelAccessor world) {
		if (world.isClientSide() && world instanceof ClientLevel) {
			AnimationTickHolder.reset();
		}
	}

	public static void onUnloadWorld(LevelAccessor world) {
		if (!world.isClientSide())
			return;
		AnimationTickHolder.reset();
	}

	public static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}

}
