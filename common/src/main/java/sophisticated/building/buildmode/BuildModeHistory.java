package sophisticated.building.buildmode;

import java.util.Objects;

/**
 * Which build mode "Activate Previous Build Mode" and "Toggle Disabled <> Previous Build Mode" switch to. Every mode
 * change is recorded, however it happened (radial menu, keys), so "previous" is always the mode that was active before
 * the current one. Pure state, no Minecraft classes (unit-tested).
 */
public final class BuildModeHistory<M> {
    private final M disabled;
    private M current;
    private M previous;
    private M beforeDisabled;

    /**
     * @param initial        the mode at start
     * @param disabled       the "Disabled" mode
     * @param beforeDisabled where the Disable toggle goes when nothing was active before
     */
    public BuildModeHistory(M initial, M disabled, M beforeDisabled) {
        this.disabled = Objects.requireNonNull(disabled);
        this.current = Objects.requireNonNull(initial);
        this.previous = initial;
        this.beforeDisabled = Objects.requireNonNull(beforeDisabled);
    }

    public M current() {
        return current;
    }

    /** Records a mode change; re-selecting the current mode changes nothing. */
    public void changeTo(M mode) {
        Objects.requireNonNull(mode);
        if (mode.equals(current)) return;
        previous = current;
        if (!current.equals(disabled)) beforeDisabled = current;
        current = mode;
    }

    /** The mode "Activate Previous Build Mode" switches to: the one active before the current one. */
    public M previous() {
        return previous;
    }

    /** The mode "Toggle Disabled <> Previous Build Mode" switches to. */
    public M disableToggleTarget() {
        return current.equals(disabled) ? beforeDisabled : disabled;
    }
}
