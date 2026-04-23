package sophisticated.building.buildmode;

import sophisticated.building.AllIcons;
import sophisticated.building.buildmode.buildmodes.Circle;
import sophisticated.building.buildmode.buildmodes.Cone;
import sophisticated.building.buildmode.buildmodes.Cube;
import sophisticated.building.buildmode.buildmodes.Cylinder;
import sophisticated.building.buildmode.buildmodes.DiagonalLine;
import sophisticated.building.buildmode.buildmodes.DiagonalWall;
import sophisticated.building.buildmode.buildmodes.Disabled;

import sophisticated.building.buildmode.buildmodes.Floor;
import sophisticated.building.buildmode.buildmodes.Line;
import sophisticated.building.buildmode.buildmodes.Pyramid;
import sophisticated.building.buildmode.buildmodes.Single;
import sophisticated.building.buildmode.buildmodes.SlopeFloor;
import sophisticated.building.buildmode.buildmodes.Sphere;
import sophisticated.building.buildmode.buildmodes.TerrainMound;
import sophisticated.building.buildmode.buildmodes.Wall;

public enum BuildModeEnum {
    DISABLED("normal", new Disabled(), BuildModeCategoryEnum.BASIC, AllIcons.I_DISABLE),
    SINGLE("normal_plus", new Single(), BuildModeCategoryEnum.BASIC, AllIcons.I_SINGLE, ModeOptions.OptionEnum.BUILD_SPEED),
    LINE("line", new Line(), BuildModeCategoryEnum.BASIC, AllIcons.I_LINE /*, OptionEnum.THICKNESS*/),
    WALL("wall", new Wall(), BuildModeCategoryEnum.BASIC, AllIcons.I_WALL, ModeOptions.OptionEnum.FILL),
    FLOOR("floor", new Floor(), BuildModeCategoryEnum.BASIC, AllIcons.I_FLOOR, ModeOptions.OptionEnum.FILL),
    CUBE("cube", new Cube(), BuildModeCategoryEnum.BASIC, AllIcons.I_CUBE, ModeOptions.OptionEnum.CUBE_FILL),
    DIAGONAL_LINE("diagonal_line", new DiagonalLine(), BuildModeCategoryEnum.DIAGONAL, AllIcons.I_DIAGONAL_LINE /*, OptionEnum.THICKNESS*/),
    DIAGONAL_WALL("diagonal_wall", new DiagonalWall(), BuildModeCategoryEnum.DIAGONAL, AllIcons.I_DIAGONAL_WALL /*, OptionEnum.FILL*/),
    SLOPE_FLOOR("slope_floor", new SlopeFloor(), BuildModeCategoryEnum.DIAGONAL, AllIcons.I_SLOPED_FLOOR, ModeOptions.OptionEnum.RAISED_EDGE),
    CIRCLE("circle", new Circle(), BuildModeCategoryEnum.CIRCULAR, AllIcons.I_CIRCLE, ModeOptions.OptionEnum.CIRCLE_START, ModeOptions.OptionEnum.FILL),
    CYLINDER("cylinder", new Cylinder(), BuildModeCategoryEnum.CIRCULAR, AllIcons.I_CYLINDER, ModeOptions.OptionEnum.CIRCLE_START, ModeOptions.OptionEnum.FILL),
    SPHERE("sphere", new Sphere(), BuildModeCategoryEnum.CIRCULAR, AllIcons.I_SPHERE, ModeOptions.OptionEnum.CIRCLE_START, ModeOptions.OptionEnum.FILL),
    PYRAMID("pyramid", new Pyramid(), BuildModeCategoryEnum.ROOF, AllIcons.I_PYRAMID, ModeOptions.OptionEnum.FILL),
    CONE("cone", new Cone(), BuildModeCategoryEnum.ROOF, AllIcons.I_CONE, ModeOptions.OptionEnum.CIRCLE_START, ModeOptions.OptionEnum.FILL),
    TERRAIN_MOUND("terrain_mound", new TerrainMound(), BuildModeCategoryEnum.TERRAIN, AllIcons.I_DOME, ModeOptions.OptionEnum.CIRCLE_START, ModeOptions.OptionEnum.FILL, ModeOptions.OptionEnum.TERRAIN_NOISE, ModeOptions.OptionEnum.TERRAIN_TYPE);

    private final String name;
    public final IBuildMode instance;
    public final BuildModeCategoryEnum category;
    public final AllIcons icon;
    public final ModeOptions.OptionEnum[] options;

    BuildModeEnum(String name, IBuildMode instance, BuildModeCategoryEnum category, AllIcons icon, ModeOptions.OptionEnum... options) {
        this.name = name;
        this.instance = instance;
        this.category = category;
        this.icon = icon;
        this.options = options;
    }

    public String getNameKey() {
        return "sophisticatedbuilding.mode." + name;
    }

    public String getDescriptionKey() {
        return "sophisticatedbuilding.modedescription." + name;
    }

}
