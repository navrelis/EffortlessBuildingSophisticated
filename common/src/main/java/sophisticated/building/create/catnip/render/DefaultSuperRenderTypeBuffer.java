package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.render.DefaultSuperRenderTypeBuffer}, MIT License, Copyright (c) 2022
 * The Create Team, see LICENSE_Ponder.txt). Minecraft 26.2 has no immediate buffer sources: each layer records its
 * vertices ({@link RecordingBufferSource}) and {@link #submit} hands them to the level's submit node collector, each
 * layer with its own submit order so that early, default and late geometry is still drawn in this order.
 */
public class DefaultSuperRenderTypeBuffer implements SuperRenderTypeBuffer {

	private static final DefaultSuperRenderTypeBuffer INSTANCE = new DefaultSuperRenderTypeBuffer();

	public static DefaultSuperRenderTypeBuffer getInstance() {
		return INSTANCE;
	}

	protected final RecordingBufferSource earlyBuffer = new RecordingBufferSource();
	protected final RecordingBufferSource defaultBuffer = new RecordingBufferSource();
	protected final RecordingBufferSource lateBuffer = new RecordingBufferSource();

	@Override
	public VertexConsumer getEarlyBuffer(RenderType type) {
		return earlyBuffer.getBuffer(type);
	}

	@Override
	public VertexConsumer getBuffer(RenderType type) {
		return defaultBuffer.getBuffer(type);
	}

	@Override
	public VertexConsumer getLateBuffer(RenderType type) {
		return lateBuffer.getBuffer(type);
	}

	/** Forgets the vertices of the previous frame. */
	public void reset() {
		earlyBuffer.reset();
		defaultBuffer.reset();
		lateBuffer.reset();
	}

	/**
	 * Submits the recorded layers with the submit orders {@code firstOrder} (early), {@code firstOrder + 1} (default) and
	 * {@code firstOrder + 2} (late): the game draws the geometry of one phase order by order, while it batches the
	 * render types of one order in no particular order.
	 */
	public void submit(SubmitNodeCollector collector, int firstOrder) {
		earlyBuffer.submit(collector.order(firstOrder));
		defaultBuffer.submit(collector.order(firstOrder + 1));
		lateBuffer.submit(collector.order(firstOrder + 2));
	}
}
