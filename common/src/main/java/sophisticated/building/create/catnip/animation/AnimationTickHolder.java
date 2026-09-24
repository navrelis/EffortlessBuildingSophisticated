package sophisticated.building.create.catnip.animation;

import net.minecraft.client.Minecraft;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.animation.AnimationTickHolder}, MIT License,
 * Copyright (c) 2022 The Create Team, see LICENSE_Ponder.txt). Ticked by
 * {@code sophisticated.building.create.events.ClientEvents}; the Ponder level special cases are
 * removed.
 */
public class AnimationTickHolder {

	private static int ticks;
	private static int pausedTicks;
	private static long lastTickNanos = System.nanoTime();

	public static void reset() {
		ticks = 0;
		pausedTicks = 0;
	}

	public static void tick() {
		lastTickNanos = System.nanoTime();
		if (!Minecraft.getInstance()
			.isPaused()) {
			ticks = (ticks + 1) % 1_728_000; // wrap around every 24 hours so we maintain enough floating point precision
		} else {
			pausedTicks = (pausedTicks + 1) % 1_728_000;
		}
	}

	public static int getTicks() {
		return getTicks(false);
	}

	public static int getTicks(boolean includePaused) {
		return includePaused ? ticks + pausedTicks : ticks;
	}

	public static float getRenderTime() {
		return getTicks() + getPartialTicks();
	}

	/**
	 * @return the fraction between the current tick to the next tick, frozen during game pause [0-1]
	 */
	public static float getPartialTicks() {
		Minecraft mc = Minecraft.getInstance();
		return mc.getTimer().getGameTimeDeltaPartialTick(false);
	}

	/**
	 * @return the fraction between the current tick to the next tick, not frozen during game pause [0-1].
	 * Catnip reads the timer's residual through a mixin accessor; here it is measured from the last
	 * client tick, which gives the same value at the normal 20 ticks per second.
	 */
	public static float getPartialTicksUI() {
		float sinceTick = (System.nanoTime() - lastTickNanos) / 50_000_000f;
		return Math.max(0f, Math.min(1f, sinceTick));
	}
}
