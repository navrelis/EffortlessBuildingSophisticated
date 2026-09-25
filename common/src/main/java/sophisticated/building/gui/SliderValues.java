package sophisticated.building.gui;

/**
 * Maps a slider position (0..1) to a config value in {@code [min, max]} and back. Values snap to multiples of
 * {@code step} counted from {@code min} (the range ends are always reachable). An {@code exponent} above 1 gives the
 * lower part of a wide range more of the slider (value = min + range * position^exponent), so e.g. 0..100000 stays
 * usable around the common values of a few thousand. Pure math, no Minecraft classes.
 */
public record SliderValues(double min, double max, double step, double exponent) {

    public SliderValues {
        if (!(max > min)) throw new IllegalArgumentException("max must be greater than min: " + min + " .. " + max);
        if (!(step > 0)) throw new IllegalArgumentException("step must be positive: " + step);
        if (!(exponent >= 1)) throw new IllegalArgumentException("exponent must be at least 1: " + exponent);
    }

    /** A linear slider. */
    public static SliderValues linear(double min, double max, double step) {
        return new SliderValues(min, max, step, 1);
    }

    /** The snapped value at a slider position (clamped to 0..1). */
    public double valueAt(double position) {
        double p = clamp(position, 0, 1);
        double raw = min + (max - min) * Math.pow(p, exponent);
        return snap(raw);
    }

    /** The slider position that shows a value (clamped to the range). */
    public double positionOf(double value) {
        double v = clamp(value, min, max);
        return Math.pow((v - min) / (max - min), 1 / exponent);
    }

    /** Nearest multiple of {@code step} from {@code min}, clamped to the range; the range ends snap to themselves. */
    public double snap(double value) {
        double v = clamp(value, min, max);
        if (v == max) return max;
        double snapped = min + Math.round((v - min) / step) * step;
        // Avoid 0.30000000000000004-style noise from the multiplication
        snapped = Math.round(snapped * 1e9) / 1e9;
        return clamp(snapped, min, max);
    }

    /** {@link #valueAt} as a whole number (integer config values). */
    public int intValueAt(double position) {
        return (int) Math.round(valueAt(position));
    }

    private static double clamp(double value, double low, double high) {
        return value < low ? low : Math.min(value, high);
    }
}
