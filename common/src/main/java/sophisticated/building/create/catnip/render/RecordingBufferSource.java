package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One vertex recorder per render type, the replacement of an immediate {@code MultiBufferSource.BufferSource}
 * (removed in Minecraft 26.2). {@link #submit} hands every render type's vertices to a submit node collector as custom
 * geometry; the game draws them when it renders the level's submits (render types with blending in the translucent
 * custom geometry phase, the others with the solid features).
 * <p>
 * The collector replays the vertices while it prepares the frame, which follows the collection of the submits in the
 * same frame. The recorders are therefore only cleared by {@link #reset}, at the start of the next frame's collection.
 */
public class RecordingBufferSource {

	private static final PoseStack IDENTITY = new PoseStack();

	private final Map<RenderType, VertexRecorder> recorders = new LinkedHashMap<>();

	public VertexConsumer getBuffer(RenderType type) {
		return recorders.computeIfAbsent(type, t -> new VertexRecorder());
	}

	/** Forgets the vertices of the previous frame. */
	public void reset() {
		recorders.values().forEach(VertexRecorder::clear);
	}

	/** Submits the vertices recorded since the last {@link #reset}, one custom geometry node per render type. */
	public void submit(OrderedSubmitNodeCollector collector) {
		recorders.forEach((type, recorder) -> {
			if (!recorder.isEmpty())
				collector.submitCustomGeometry(IDENTITY, type, (pose, consumer) -> recorder.replay(consumer));
		});
	}
}
