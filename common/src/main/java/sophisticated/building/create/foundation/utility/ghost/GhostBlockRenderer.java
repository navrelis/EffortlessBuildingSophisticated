package sophisticated.building.create.foundation.utility.ghost;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import sophisticated.building.create.catnip.render.SuperRenderTypeBuffer;
import sophisticated.building.create.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.platform.ClientServices;

import java.util.List;

public abstract class GhostBlockRenderer {

	private static final GhostBlockRenderer STANDARD = new DefaultGhostBlockRenderer();

	public static GhostBlockRenderer standard() {
		return STANDARD;
	}

	private static final GhostBlockRenderer TRANSPARENT = new TransparentGhostBlockRenderer();

	public static GhostBlockRenderer transparent() {
		return TRANSPARENT;
	}

	public abstract void render(PoseStack ms, SuperRenderTypeBuffer buffer, GhostBlockParams params);

	/** The block model of {@code state} (Minecraft 26.1: from the model manager's block state model set). */
	protected static BlockStateModel blockModel(BlockState state) {
		return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
	}

	// ModelBlockRenderer (Minecraft 26.1 has no immediate model renderer any more: every quad of every part, tinted
	// with the given colour, through VertexConsumer#putBakedQuad)
	protected static void renderModel(PoseStack.Pose pose, VertexConsumer consumer, BlockState state, BlockStateModel model,
		float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
		QuadInstance instance = new QuadInstance();
		instance.setColor(ARGB.colorFromFloat(Mth.clamp(alpha, 0.0F, 1.0F), Mth.clamp(red, 0.0F, 1.0F),
			Mth.clamp(green, 0.0F, 1.0F), Mth.clamp(blue, 0.0F, 1.0F)));
		instance.setLightCoords(packedLight);
		instance.setOverlayCoords(packedOverlay);
		for (BlockStateModelPart part : ClientServices.CLIENT.collectModelParts(model, state, RandomSource.create(42L))) {
			for (Direction direction : Direction.values()) {
				renderQuadList(pose, consumer, part.getQuads(direction), instance);
			}
			renderQuadList(pose, consumer, part.getQuads(null), instance);
		}
	}

	private static void renderQuadList(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads, QuadInstance instance) {
		for (BakedQuad quad : quads) {
			consumer.putBakedQuad(pose, quad, instance);
		}
	}

	private static class DefaultGhostBlockRenderer extends GhostBlockRenderer {

		@Override
		public void render(PoseStack ms, SuperRenderTypeBuffer buffer, GhostBlockParams params) {
			BlockState state = params.state;
			BlockPos pos = params.pos;

			BlockStateModel model = blockModel(state);

			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());

			VertexConsumer vb = buffer.getEarlyBuffer(RenderTypes.solidMovingBlock());
			renderModel(ms.last(), vb, state, model, 1f, 1f, 1f, 1f, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

			ms.popPose();
		}

	}

	private static class TransparentGhostBlockRenderer extends GhostBlockRenderer {

		@Override
		public void render(PoseStack ms, SuperRenderTypeBuffer buffer, GhostBlockParams params) {
			Minecraft mc = Minecraft.getInstance();

			BlockState state = params.state;
			BlockPos pos = params.pos;
			float alpha = params.alphaSupplier.get()/*  * .75f* PlacementHelpers.getCurrentAlpha()*/;
			float scale = params.scaleSupplier.get();
			Color color = params.rgbSupplier.get();

			BlockStateModel model = blockModel(state);
			// Translucent block models outside the chunk renderer (1.21.6+: the chunk layers are no render types)
			VertexConsumer vb = buffer.getEarlyBuffer(RenderTypes.translucentMovingBlock());

			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());

			ms.translate(.5, .5, .5);
			ms.scale(scale, scale, scale);
			ms.translate(-.5, -.5, -.5);

			renderModel(ms.last(), vb, state, model, color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha,
				LevelRenderer.getLightCoords(mc.level, pos), OverlayTexture.NO_OVERLAY);

			ms.popPose();
		}

	}

}
