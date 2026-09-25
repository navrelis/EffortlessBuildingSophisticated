package sophisticated.building.gui;

import java.util.function.ToIntFunction;

/**
 * How a screen title is drawn into a fixed width: at full size if it fits, scaled down to at most
 * {@link #MIN_SCALE}, and below that at {@link #MIN_SCALE} with the end cut off and "..." appended (the screen then
 * shows the full title as a tooltip). Pure layout math: the text width comes from a function (the font in the game,
 * a fixed width per character in tests).
 *
 * @param scale     the scale to draw the text with (1 = full size)
 * @param text      the text to draw ({@code full} or its ellipsized prefix)
 * @param truncated whether {@code text} was cut off
 */
public final class TitleFit {

    public static final float MIN_SCALE = 0.6f;
    public static final String ELLIPSIS = "...";

    private final float scale;
    private final String text;
    private final boolean truncated;

    public TitleFit(float scale, String text, boolean truncated) {
        this.scale = scale;
        this.text = text;
        this.truncated = truncated;
    }

    public float scale() {
        return scale;
    }

    public String text() {
        return text;
    }

    public boolean truncated() {
        return truncated;
    }

    /** Fits {@code full} into {@code availableWidth} (same unit as {@code width}, GUI pixels in the game). */
    public static TitleFit fit(String full, int availableWidth, ToIntFunction<String> width) {
        int fullWidth = width.applyAsInt(full);
        if (fullWidth <= availableWidth) return new TitleFit(1f, full, false);
        float scale = availableWidth / (float) fullWidth;
        if (scale >= MIN_SCALE) return new TitleFit(scale, full, false);

        // At the smallest scale the text may be availableWidth / MIN_SCALE wide (unscaled)
        int maxUnscaled = (int) Math.floor(availableWidth / MIN_SCALE);
        int end = full.length();
        while (end > 0 && width.applyAsInt(stripTrailing(full.substring(0, end)) + ELLIPSIS) > maxUnscaled) {
            end--;
        }
        return new TitleFit(MIN_SCALE, stripTrailing(full.substring(0, end)) + ELLIPSIS, true);
    }

    /** {@code String#stripTrailing} of Java 11. */
    private static String stripTrailing(String text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    /** Width of the drawn text in the caller's unit, i.e. what the title actually occupies on screen. */
    public float drawnWidth(ToIntFunction<String> width) {
        return width.applyAsInt(text) * scale;
    }
}
