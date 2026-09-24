package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.render.SuperRenderTypeBuffer}, MIT License, Copyright (c) 2022
 * The Create Team, see LICENSE_Ponder.txt). Minecraft 26.2 removed {@code MultiBufferSource}: this is no buffer source
 * any more, only the three layers the outliner and the ghost blocks write into; drawing happens by submitting them
 * ({@link DefaultSuperRenderTypeBuffer#submit}).
 */
public interface SuperRenderTypeBuffer {
	VertexConsumer getEarlyBuffer(RenderType type);

	VertexConsumer getBuffer(RenderType type);

	VertexConsumer getLateBuffer(RenderType type);
}
