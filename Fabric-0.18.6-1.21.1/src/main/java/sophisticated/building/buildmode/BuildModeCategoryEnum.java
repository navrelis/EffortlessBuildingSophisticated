package sophisticated.building.buildmode;

import sophisticated.building.utilities.Color;

public enum BuildModeCategoryEnum {
    BASIC(new Color(0f, .5f, 1f, .8f)),
    DIAGONAL(new Color(0.56f, 0.28f, 0.87f, .8f)),
    CIRCULAR(new Color(0.29f, 0.76f, 0.3f, 1f)),
    ROOF(new Color(0.83f, 0.87f, 0.23f, .8f)),
    TERRAIN(new Color(0.55f, 0.35f, 0.15f, .8f)); // Brown/earthy color for terrain

    public final Color color;

    BuildModeCategoryEnum(Color color) {
        this.color = color;
    }
}
