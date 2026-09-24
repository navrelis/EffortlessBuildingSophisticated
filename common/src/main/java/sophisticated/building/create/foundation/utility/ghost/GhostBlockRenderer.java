package sophisticated.building.create.foundation.utility.ghost;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import sophisticated.building.create.catnip.render.SuperRenderTypeBuffer;
import sophisticated.building.create.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.platform.ClientServices;

import javax.annotation.Nullable;
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

	private static class DefaultGhostBlockRenderer extends GhostBlockRenderer {

		@Override
		public void render(PoseStack ms, SuperRenderTypeBuffer buffer, GhostBlockParams params) {
			BlockRenderDispatcher dispatcher = Minecraft.getInstance()
				.getBlockRenderer();

			BlockState state = params.state;
			BlockPos pos = params.pos;

			BlockStateModel model = dispatcher.getBlockModel(state);

			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());

			VertexConsumer vb = buffer.getEarlyBuffer(RenderType.solid());
			ModelBlockRenderer.renderModel(ms.last(), vb, model, 1f, 1f, 1f, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

			ms.popPose();
		}

	}

	private static class TransparentGhostBlockRenderer extends GhostBlockRenderer {

		@Override
		public void render(PoseStack ms, SuperRenderTypeBuffer buffer, GhostBlockParams params) {
			Minecraft mc = Minecraft.getInstance();
			BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

			BlockState state = params.state;
			BlockPos pos = params.pos;
			float alpha = params.alphaSupplier.get()/*  * .75f* PlacementHelpers.getCurrentAlpha()*/;
			float scale = params.scaleSupplier.get();
			Color color = params.rgbSupplier.get();

			BlockStateModel model = dispatcher.getBlockModel(state);
			RenderType layer = RenderType.translucent();
			VertexConsumer vb = buffer.getEarlyBuffer(layer);

			ms.pushPose();
			ms.translate(pos.getX(), pos.getY(), pos.getZ());

			ms.translate(.5, .5, .5);
			ms.scale(scale, scale, scale);
			ms.translate(-.5, -.5, -.5);

			renderModel(ms.last(), vb, state, model, color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat(), alpha,
				LevelRenderer.getLightColor(mc.level, pos), OverlayTexture.NO_OVERLAY, layer);

			ms.popPose();
		}

		// ModelBlockRenderer
		public void renderModel(PoseStack.Pose pose, VertexConsumer consumer,
			@Nullable BlockState state, BlockStateModel model, float red, float green, float blue,
			float alpha, int packedLight, int packedOverlay, RenderType renderType) {
			for (BlockModelPart part : ClientServices.CLIENT.collectModelParts(model, state, RandomSource.create(42L), renderType)) {
				for (Direction direction : Direction.values()) {
					renderQuadList(pose, consumer, red, green, blue, alpha, part.getQuads(direction), packedLight, packedOverlay);
				}
				renderQuadList(pose, consumer, red, green, blue, alpha, part.getQuads(null), packedLight, packedOverlay);
			}
		}

		// ModelBlockRenderer
		private static void renderQuadList(PoseStack.Pose pose, VertexConsumer consumer,
			float red, float green, float blue, float alpha, List<BakedQuad> quads,
			int packedLight, int packedOverlay) {
			for (BakedQuad quad : quads) {
				float f;
				float f1;
				float f2;
//				if (quad.isTinted()) {
					f = Mth.clamp(red, 0.0F, 1.0F);
					f1 = Mth.clamp(green, 0.0F, 1.0F);
					f2 = Mth.clamp(blue, 0.0F, 1.0F);
//				} else {
//					f = 1.0F;
//					f1 = 1.0F;
//					f2 = 1.0F;
//				}

				ClientServices.CLIENT.putQuad(consumer, pose, quad, f, f1, f2, alpha, packedLight, packedOverlay);
			}

		}

	}

}
