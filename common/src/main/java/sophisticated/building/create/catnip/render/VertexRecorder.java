package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.ARGB;

import java.util.Arrays;

/**
 * A vertex consumer that keeps the vertices written to it and writes them again into another consumer later
 * ({@link #replay}). Minecraft 26.2 removed {@code MultiBufferSource}: geometry outside the chunk and entity renderers
 * is submitted to the {@code SubmitNodeCollector} as a callback that gets a vertex consumer only when the frame is
 * prepared. The mod's renderers record their vertices here while the frame's submits are collected, and the callback
 * replays them. Positions and normals are recorded as written, so the callback's pose is not applied again.
 */
public class VertexRecorder implements VertexConsumer {

	private static final int COLOR = 1;
	private static final int UV = 1 << 1;
	private static final int UV1 = 1 << 2;
	private static final int UV2 = 1 << 3;
	private static final int NORMAL = 1 << 4;
	private static final int LINE_WIDTH = 1 << 5;

	/** x, y, z, u, v, normal x, y, z, line width */
	private static final int FLOATS = 9;
	/** set attributes, colour (ARGB), uv1 (overlay), uv2 (light) */
	private static final int INTS = 4;

	private float[] floats = new float[FLOATS * 64];
	private int[] ints = new int[INTS * 64];
	private int vertexCount;

	public boolean isEmpty() {
		return vertexCount == 0;
	}

	public void clear() {
		vertexCount = 0;
	}

	/** Writes every recorded vertex, with the attributes that were set on it, into {@code consumer}. */
	public void replay(VertexConsumer consumer) {
		for (int vertex = 0; vertex < vertexCount; vertex++) {
			int f = vertex * FLOATS;
			int i = vertex * INTS;
			int set = ints[i];
			consumer.addVertex(floats[f], floats[f + 1], floats[f + 2]);
			if ((set & COLOR) != 0)
				consumer.setColor(ints[i + 1]);
			if ((set & UV) != 0)
				consumer.setUv(floats[f + 3], floats[f + 4]);
			if ((set & UV1) != 0)
				consumer.setUv1(ints[i + 2] & 0xFFFF, ints[i + 2] >>> 16);
			if ((set & UV2) != 0)
				consumer.setUv2(ints[i + 3] & 0xFFFF, ints[i + 3] >>> 16);
			if ((set & NORMAL) != 0)
				consumer.setNormal(floats[f + 5], floats[f + 6], floats[f + 7]);
			if ((set & LINE_WIDTH) != 0)
				consumer.setLineWidth(floats[f + 8]);
		}
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		if ((vertexCount + 1) * FLOATS > floats.length) {
			floats = Arrays.copyOf(floats, floats.length * 2);
			ints = Arrays.copyOf(ints, ints.length * 2);
		}
		int f = vertexCount * FLOATS;
		floats[f] = x;
		floats[f + 1] = y;
		floats[f + 2] = z;
		ints[vertexCount * INTS] = 0;
		vertexCount++;
		return this;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		return setColor(ARGB.color(a, r, g, b));
	}

	@Override
	public VertexConsumer setColor(int color) {
		int i = current(COLOR) * INTS;
		ints[i + 1] = color;
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		int f = current(UV) * FLOATS;
		floats[f + 3] = u;
		floats[f + 4] = v;
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		ints[current(UV1) * INTS + 2] = (u & 0xFFFF) | (v << 16);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		ints[current(UV2) * INTS + 3] = (u & 0xFFFF) | (v << 16);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		int f = current(NORMAL) * FLOATS;
		floats[f + 5] = x;
		floats[f + 6] = y;
		floats[f + 7] = z;
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		floats[current(LINE_WIDTH) * FLOATS + 8] = width;
		return this;
	}

	/** Index of the vertex being written, marking {@code attribute} as set on it. */
	private int current(int attribute) {
		if (vertexCount == 0)
			throw new IllegalStateException("Not building a vertex");
		int vertex = vertexCount - 1;
		ints[vertex * INTS] |= attribute;
		return vertex;
	}
}
