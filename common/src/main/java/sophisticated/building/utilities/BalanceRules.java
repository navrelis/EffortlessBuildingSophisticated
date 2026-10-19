package sophisticated.building.utilities;

/**
 * Pure rules of balanced survival breaking (server config section {@code BalancedBreaking}, client toggle
 * {@code Gameplay.balancedBreaking}): when it is on, who it applies to, what one mined block costs and why a request is
 * refused. No Minecraft imports and no config access so this class is directly unit-testable (see
 * {@code BalanceRulesTest}); the server glue is {@code BalancedBreaking}. Keep Java 8 compatible.
 */
public final class BalanceRules {

	/** Operations that mine fewer blocks are exempt from every balance rule: they equal a vanilla break. */
	public static final int MIN_BLOCKS = 2;

	/** Why a balanced operation was refused when the request arrived. */
	public enum Refusal {
		NONE, COOLDOWN, HUNGER
	}

	private BalanceRules() {
	}

	/**
	 * Whether balanced breaking is on for the players of a server. A dedicated server only follows its server config;
	 * an integrated server (singleplayer, a LAN world) also follows the host's client toggle.
	 */
	public static boolean isEnabled(boolean serverConfigEnabled, boolean dedicatedServer, boolean hostClientSetting) {
		return serverConfigEnabled || (!dedicatedServer && hostClientSetting);
	}

	/** Whether an operation that mines {@code minedBlocks} blocks is balanced: never in creative, never for one block. */
	public static boolean applies(boolean enabled, boolean creative, int minedBlocks) {
		return enabled && !creative && minedBlocks >= MIN_BLOCKS;
	}

	/** Durability uses one mined block costs a tool: the multiplier when balanced (at least 1), else 1. */
	public static int usesPerBlock(boolean balanced, int durabilityMultiplier) {
		return balanced ? Math.max(1, durabilityMultiplier) : 1;
	}

	/** A balanced operation needs at least {@code minFoodLevel} food (0..20, two per shank). */
	public static boolean tooHungry(int foodLevel, int minFoodLevel) {
		return foodLevel < minFoodLevel;
	}

	/** The request-time check: a running cooldown first, then hunger. */
	public static Refusal check(long cooldownRemainingMillis, int foodLevel, int minFoodLevel) {
		if (cooldownRemainingMillis > 0) {
			return Refusal.COOLDOWN;
		}
		if (tooHungry(foodLevel, minFoodLevel)) {
			return Refusal.HUNGER;
		}
		return Refusal.NONE;
	}

	/** Whole seconds shown for a remaining cooldown, rounded up so that 0.2 s left still shows 1 s. */
	public static long displaySeconds(long remainingMillis) {
		return remainingMillis <= 0 ? 0 : (remainingMillis + 999) / 1000;
	}

	/**
	 * The cooldown settings from the config's units (seconds, minutes). The maximum is never below the base, so a
	 * maximum configured lower than the base cannot cut a cooldown short.
	 */
	public static BalanceCooldown.Settings settings(int baseSeconds, int windowMinutes, int freeUses, int stepSeconds, int maxSeconds) {
		long base = Math.max(0, baseSeconds) * 1000L;
		long max = Math.max(base, Math.max(0, maxSeconds) * 1000L);
		return new BalanceCooldown.Settings(base, Math.max(0, windowMinutes) * 60_000L, Math.max(0, freeUses),
				Math.max(0, stepSeconds) * 1000L, max);
	}
}
