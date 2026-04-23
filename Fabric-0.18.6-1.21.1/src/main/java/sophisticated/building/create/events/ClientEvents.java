package sophisticated.building.create.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.levelWrappers.WrappedClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.LevelAccessor;
import sophisticated.building.create.Create;
import sophisticated.building.create.CreateClient;
import sophisticated.building.create.foundation.utility.CameraAngleAnimationService;

public class ClientEvents {

	private static final String ITEM_PREFIX = "item." + Create.ID;
	private static final String BLOCK_PREFIX = "block." + Create.ID;

	public static void onTick() {
		if (!isGameActive()) return;

		AnimationTickHolder.tick();

		CreateClient.GHOST_BLOCKS.tickGhosts();
//		CreateClient.OUTLINER.tickOutlines();
		CameraAngleAnimationService.tick();
	}

	public static void onLoadWorld(LevelAccessor world) {
		if (world.isClientSide() && world instanceof ClientLevel && !(world instanceof WrappedClientLevel)) {
			CreateClient.invalidateRenderers();
			AnimationTickHolder.reset();
		}
	}

	public static void onUnloadWorld(LevelAccessor world) {
		if (!world.isClientSide())
			return;
		CreateClient.invalidateRenderers();
		AnimationTickHolder.reset();
	}

	public static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}

}

