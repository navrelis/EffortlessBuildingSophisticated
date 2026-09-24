package sophisticated.building;

import sophisticated.building.gui.ScreenElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Icon definitions for the mod's GUI.
 */
public class AllIcons implements ScreenElement {
    
    public static final Identifier ICON_ATLAS = SophisticatedBuilding.asResource("textures/gui/icons.png");
    public static final int ICON_ATLAS_SIZE = 256;
    private static int x = 0, y = -1;
    private int iconX;
    private int iconY;
    
    public static final AllIcons
    I_SETTINGS = newRow(),
    I_UNDO = next(),
    I_REDO = next(),
    I_REPLACE = next(),
    I_REPLACE_AIR = next(),
    I_REPLACE_BLOCKS_AND_AIR = next(),
    I_REPLACE_BLOCKS = next(),
    I_REPLACE_OFFHAND_FILTERED = next(),
    I_PROTECT_TILE_ENTITIES = next();

    
    public static final AllIcons
    I_DISABLE = newRow(),
    I_SINGLE = next(),
    I_LINE = next(),
    I_WALL = next(),
    I_FLOOR = next(),
    I_CUBE = next(),
    I_DIAGONAL_LINE = next(),
    I_DIAGONAL_WALL = next(),
    I_SLOPED_FLOOR = next(),
    I_CIRCLE = next(),
    I_CYLINDER = next(),
    I_SPHERE = next(),
    I_PYRAMID = next(),
    I_CONE = next(),
    I_DOME = next();
    
    public static final AllIcons
    I_NORMAL_SPEED = newRow(),
    I_FAST_SPEED = next(),
    I_FILLED = next(),
    I_HOLLOW = next(),
    I_CUBE_FILLED = next(),
    I_CUBE_HOLLOW = next(),
    I_CUBE_SKELETON = next(),
    I_SHORT_EDGE = next(),
    I_LONG_EDGE = next(),
    I_CIRCLE_START_CORNER = next(),
    I_CIRCLE_START_CENTER = next(),
    I_THICKNESS_1 = next(),
    I_THICKNESS_3 = next(),
    I_THICKNESS_5 = next();
    
    public static final AllIcons
    I_PLAYER = newRow(),
    I_BLOCK_CENTER = next(),
    I_BLOCK_CORNER = next(),
    I_HIDE_LINES = next(),
    I_SHOW_LINES = next(),
    I_HIDE_AREAS = next(),
    I_SHOW_AREAS = next(),
    I_X_OFF = next(),
    I_X_ON = next(),
    I_Y_OFF = next(),
    I_Y_ON = next(),
    I_Z_OFF = next(),
    I_Z_ON = next(),
    I_ALTERNATE_OFF = next(),
    I_ALTERNATE_ON = next();
    
    public static final AllIcons
    I_MINI_PREVIEW = newRow(),
    I_TERRAIN_NOISE_OFF = next(),
    I_TERRAIN_NOISE_ON = next(),
    I_TERRAIN_MOUND = next(),
    I_TERRAIN_SLOPE = next(),
    I_TERRAIN_FLAT = next(),
    I_TERRAIN_MOUNTAIN = next(),
    I_TERRAIN_WALL = next();
    
    
    public AllIcons(int x, int y) {
        iconX = x * 16;
        iconY = y * 16;
    }
    
    private static AllIcons next() {
        return new AllIcons(++x, y);
    }
    
    private static AllIcons newRow() {
        return new AllIcons(x = 0, ++y);
    }

    public void render(GuiGraphicsExtractor guiGraphics, int x, int y) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON_ATLAS, x, y, iconX, iconY, 16, 16, 256, 256);
    }
}
