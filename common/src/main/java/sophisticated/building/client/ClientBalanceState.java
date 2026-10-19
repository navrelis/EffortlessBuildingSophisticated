package sophisticated.building.client;

/**
 * Client-side holder for the player's balanced breaking state, populated only by {@code BalanceStatePacket} from the
 * server (login, respawn, dimension change, every accepted or refused balanced operation, and when the server's
 * effective setting changes). The server is authoritative: the client never predicts a cooldown, it only counts the
 * last received one down on its own clock for the HUD. Cleared when leaving the world or server.
 * No loader imports - callable from common client code on either loader.
 */
public class ClientBalanceState {
    private static volatile boolean active = false;
    private static volatile long cooldownEndMillis = 0L;
    private static volatile int usesInWindow = 0;

    private ClientBalanceState() {
    }

    public static void set(boolean newActive, long cooldownRemainingMillis, int newUsesInWindow) {
        active = newActive;
        cooldownEndMillis = cooldownRemainingMillis > 0 ? System.currentTimeMillis() + cooldownRemainingMillis : 0L;
        usesInWindow = newUsesInWindow;
    }

    /** Whether balanced breaking is on for this player's server (game mode not considered). */
    public static boolean isActive() {
        return active;
    }

    /** Remaining cooldown of the last state the server sent, counted down locally; 0 when none. */
    public static long remainingMillis() {
        long end = cooldownEndMillis;
        return end == 0L ? 0L : Math.max(0L, end - System.currentTimeMillis());
    }

    public static int getUsesInWindow() {
        return usesInWindow;
    }

    public static void clear() {
        active = false;
        cooldownEndMillis = 0L;
        usesInWindow = 0;
    }
}
