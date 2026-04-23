package sophisticated.building.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.utilities.BlockEntry;

import java.util.HashSet;

/**
 * Catnip rendering bridge; no-op when unavailable.
 */
public class CatnipRenderHelper {
    
    private static Boolean catnipAvailable = null;
    
    /**
     * Check if Catnip rendering is available.
     */
    public static boolean isCatnipAvailable() {
        if (catnipAvailable == null) {
            catnipAvailable = CompatHelper.isCatnipLoaded();
        }
        return catnipAvailable;
    }
    
    /**
     * Show a cluster outline using Catnip's Outliner.
     */
    public static void showCluster(Object id, HashSet<BlockPos> coordinates, String textureType, 
                                    float lineWidth, float r, float g, float b, float a) {
        if (!isCatnipAvailable()) return;
        
        try {
            showClusterInternal(id, coordinates, textureType, lineWidth, r, g, b, a);
        } catch (NoClassDefFoundError ignored) {
            catnipAvailable = false;
        }
    }
    
    private static void showClusterInternal(Object id, HashSet<BlockPos> coordinates, String textureType,
                                             float lineWidth, float r, float g, float b, float a) {
        net.createmod.catnip.outliner.Outliner outliner = net.createmod.catnip.outliner.Outliner.getInstance();
        net.createmod.catnip.theme.Color color = new net.createmod.catnip.theme.Color(r, g, b, a);
        
        var outline = outliner.showCluster(id, coordinates)
                .disableLineNormals()
                .lineWidth(lineWidth)
                .colored(color);
        
        // Apply texture based on type
        switch (textureType) {
            case "checkered":
                outline.withFaceTexture(sophisticated.building.create.AllSpecialTextures.CHECKERED);
                break;
            case "highlight_checkered":
                outline.withFaceTexture(sophisticated.building.create.AllSpecialTextures.HIGHLIGHT_CHECKERED);
                break;
            case "thin_checkered":
                outline.withFaceTexture(sophisticated.building.create.AllSpecialTextures.THIN_CHECKERED);
                break;
        }
    }
    
    /**
     * Show an AABB outline.
     */
    public static void showAABB(Object id, AABB aabb, float lineWidth, int color) {
        if (!isCatnipAvailable()) return;
        
        try {
            showAABBInternal(id, aabb, lineWidth, color);
        } catch (NoClassDefFoundError ignored) {
            catnipAvailable = false;
        }
    }
    
    private static void showAABBInternal(Object id, AABB aabb, float lineWidth, int color) {
        net.createmod.catnip.outliner.Outliner.getInstance().showAABB(id, aabb)
                .disableLineNormals()
                .lineWidth(lineWidth)
                .colored(color);
    }
    
    /**
     * Show a ghost block preview.
     */
    public static void showGhostBlock(String slot, BlockState state, BlockPos pos, 
                                       float scale, float alpha, boolean isRed) {
        if (!isCatnipAvailable()) return;
        
        try {
            showGhostBlockInternal(slot, state, pos, scale, alpha, isRed);
        } catch (NoClassDefFoundError ignored) {
            catnipAvailable = false;
        }
    }
    
    private static void showGhostBlockInternal(String slot, BlockState state, BlockPos pos,
                                                float scale, float alpha, boolean isRed) {
        net.createmod.catnip.theme.Color color = isRed ? 
                net.createmod.catnip.theme.Color.RED : 
                net.createmod.catnip.theme.Color.WHITE;
        
        sophisticated.building.create.CreateClient.GHOST_BLOCKS.showGhostState(slot, state)
                .at(pos)
                .scale(scale)
                .alpha(alpha)
                .colored(color);
    }
    
    /**
     * Keep an outline for a certain number of ticks.
     */
    public static void keepOutline(BlockPos pos, int ticks) {
        if (!isCatnipAvailable()) return;
        
        try {
            keepOutlineInternal(pos, ticks);
        } catch (NoClassDefFoundError ignored) {
            catnipAvailable = false;
        }
    }
    
    private static void keepOutlineInternal(BlockPos pos, int ticks) {
        net.createmod.catnip.outliner.Outliner.getInstance()
                .keep(net.createmod.catnip.data.Pair.of(pos, ticks));
    }
}
