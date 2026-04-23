package sophisticated.building.utilities;

public class Color {
    public static final Color TRANSPARENT_BLACK = new Color(0, 0, 0, 0);
    public static final Color WHITE = new Color(1f, 1f, 1f, 1f);
    public static final Color BLACK = new Color(0f, 0f, 0f, 1f);
    
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    
    /**
     * Create a color from float components (0.0 - 1.0 range)
     */
    public Color(float red, float green, float blue, float alpha) {
        this.red = clamp(red, 0f, 1f);
        this.green = clamp(green, 0f, 1f);
        this.blue = clamp(blue, 0f, 1f);
        this.alpha = clamp(alpha, 0f, 1f);
    }
    
    /**
     * Create a color from an ARGB integer (0xAARRGGBB format)
     */
    public Color(int argb) {
        this.alpha = ((argb >> 24) & 0xFF) / 255f;
        this.red = ((argb >> 16) & 0xFF) / 255f;
        this.green = ((argb >> 8) & 0xFF) / 255f;
        this.blue = (argb & 0xFF) / 255f;
    }
    
    /**
     * Create a color from an ARGB integer with alpha override
     */
    public Color(int rgb, boolean hasAlpha) {
        if (hasAlpha) {
            this.alpha = ((rgb >> 24) & 0xFF) / 255f;
        } else {
            this.alpha = 1f;
        }
        this.red = ((rgb >> 16) & 0xFF) / 255f;
        this.green = ((rgb >> 8) & 0xFF) / 255f;
        this.blue = (rgb & 0xFF) / 255f;
    }
    
    public float getRed() {
        return red;
    }
    
    public float getGreen() {
        return green;
    }
    
    public float getBlue() {
        return blue;
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public int getRedAsInt() {
        return (int) (red * 255);
    }
    
    public int getGreenAsInt() {
        return (int) (green * 255);
    }
    
    public int getBlueAsInt() {
        return (int) (blue * 255);
    }
    
    public int getAlphaAsInt() {
        return (int) (alpha * 255);
    }
    
    /**
     * Convert to ARGB integer (0xAARRGGBB format)
     */
    public int toARGB() {
        return (getAlphaAsInt() << 24) | (getRedAsInt() << 16) | (getGreenAsInt() << 8) | getBlueAsInt();
    }
    
    /**
     * Convert to RGB integer (0x00RRGGBB format)
     */
    public int toRGB() {
        return (getRedAsInt() << 16) | (getGreenAsInt() << 8) | getBlueAsInt();
    }
    
    /**
     * Create a copy with different alpha
     */
    public Color withAlpha(float newAlpha) {
        return new Color(red, green, blue, newAlpha);
    }
    
    /**
     * Mix this color with another color
     */
    public Color mixWith(Color other, float ratio) {
        float r = ratio;
        float ir = 1f - ratio;
        return new Color(
            red * ir + other.red * r,
            green * ir + other.green * r,
            blue * ir + other.blue * r,
            alpha * ir + other.alpha * r
        );
    }
    
    private static float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
    
    @Override
    public String toString() {
        return String.format("Color[r=%.2f, g=%.2f, b=%.2f, a=%.2f]", red, green, blue, alpha);
    }
}
