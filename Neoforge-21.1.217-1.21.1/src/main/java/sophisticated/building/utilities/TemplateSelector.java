package sophisticated.building.utilities;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Picks the template stack for each survival placement of one block set. Free of Minecraft types so it
 * can be unit-tested.
 * <p>
 * Stacks with data are consumed individually by the caller right after placing, so they are returned as
 * long as they are not empty. Plain stacks are only removed in bulk after the whole set, so every pick is
 * reserved here; a plain stack whose count is used up by reservations (and by the held build anchor) is
 * skipped, which keeps a later stack with data from being charged to the bulk removal.
 */
public class TemplateSelector<S> {
    private final ToIntFunction<S> count;
    private final Predicate<S> hasData;
    private final Map<S, Integer> reservedPlain = new IdentityHashMap<>();

    public TemplateSelector(ToIntFunction<S> count, Predicate<S> hasData) {
        this.count = count;
        this.hasData = hasData;
    }

    /**
     * @param candidates stacks in search order
     * @param anchor     stack holding the build anchor, or null
     * @param anchorCount number of items of {@code anchor} that must stay in place
     * @return the first usable candidate, or null if none is left
     */
    @Nullable
    public S select(List<S> candidates, @Nullable S anchor, int anchorCount) {
        for (S candidate : candidates) {
            int available = count.applyAsInt(candidate);
            if (available <= 0) continue;
            if (hasData.test(candidate)) return candidate;

            available -= reservedPlain.getOrDefault(candidate, 0);
            if (candidate == anchor) available -= anchorCount;
            if (available > 0) {
                reservedPlain.merge(candidate, 1, Integer::sum);
                return candidate;
            }
        }
        return null;
    }
}
