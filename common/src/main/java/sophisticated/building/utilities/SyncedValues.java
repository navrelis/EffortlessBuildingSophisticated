package sophisticated.building.utilities;

import java.util.Arrays;

/**
 * Values the server sent to the client for its own config values (the common config limits of the power levels), used
 * by the client instead of its local file while connected, so it builds with the server's limits. Pure (see
 * {@code SyncedValuesTest}).
 */
public final class SyncedValues {

    private volatile int[] values;

    /** The server's values, in the order both sides list them; replaces earlier ones. */
    public void apply(int[] serverValues) {
        values = serverValues == null ? null : Arrays.copyOf(serverValues, serverValues.length);
    }

    /** Back to the local values (after leaving a world or server). */
    public void clear() {
        values = null;
    }

    public boolean isSynced() {
        return values != null;
    }

    /** The value at {@code index}: the server's on the client side while synced, else the local one. */
    public int pick(boolean clientSide, int index, int local) {
        int[] current = values;
        if (!clientSide || current == null || index < 0 || index >= current.length) return local;
        return current[index];
    }
}
