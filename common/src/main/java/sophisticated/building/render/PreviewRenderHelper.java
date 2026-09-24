package sophisticated.building.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import sophisticated.building.create.AllSpecialTextures;
import sophisticated.building.create.CreateClient;
import sophisticated.building.create.catnip.data.Pair;
import sophisticated.building.create.catnip.outliner.Outliner;
import sophisticated.building.create.catnip.theme.Color;

import java.util.HashSet;

/**
 * Preview outlines and ghost blocks, drawn by the outliner and ghost block renderer vendored from
 * Catnip/Create (see {@link RenderHandler}).
 */
public class PreviewRenderHelper {

    /**
     * Show a cluster outline.
     */
    public static void showCluster(Object id, HashSet<BlockPos> coordinates, String textureType,
                                    float lineWidth, float r, float g, float b, float a) {
        Color color = new Color(r, g, b, a);

        var outline = Outliner.getInstance().showCluster(id, coordinates)
                .disableLineNormals()
                .lineWidth(lineWidth)
                .colored(color);

        // Apply texture based on type
        switch (textureType) {
            case "checkered":
                outline.withFaceTexture(AllSpecialTextures.CHECKERED);
                break;
            case "highlight_checkered":
                outline.withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
                break;
            case "thin_checkered":
                outline.withFaceTexture(AllSpecialTextures.THIN_CHECKERED);
                break;
        }
    }

    /**
     * Show an AABB outline.
     */
    public static void showAABB(Object id, AABB aabb, float lineWidth, int color) {
        Outliner.getInstance().showAABB(id, aabb)
                .disableLineNormals()
                .lineWidth(lineWidth)
                .colored(color);
    }

    /**
     * Show a ghost block preview.
     */
    public static void showGhostBlock(String slot, BlockState state, BlockPos pos,
                                       float scale, float alpha, boolean isRed) {
        Color color = isRed ? Color.RED : Color.WHITE;

        CreateClient.GHOST_BLOCKS.showGhostState(slot, state)
                .at(pos)
                .scale(scale)
                .alpha(alpha)
                .colored(color);
    }

    /**
     * Keep an outline for a certain number of ticks.
     */
    public static void keepOutline(BlockPos pos, int ticks) {
        Outliner.getInstance().keep(Pair.of(pos, ticks));
    }
}
