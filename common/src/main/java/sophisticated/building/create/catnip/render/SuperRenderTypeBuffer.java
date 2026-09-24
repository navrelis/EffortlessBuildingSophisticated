package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.render.SuperRenderTypeBuffer}, MIT License, Copyright (c) 2022
 * The Create Team, see LICENSE_Ponder.txt).
 */
public interface SuperRenderTypeBuffer extends MultiBufferSource {
	VertexConsumer getEarlyBuffer(RenderType type);

	VertexConsumer getBuffer(RenderType type);

	VertexConsumer getLateBuffer(RenderType type);

	void draw();

	void draw(RenderType type);
}
