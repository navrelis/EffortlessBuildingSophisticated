package sophisticated.building.platform.services;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/**
 * Client-only loader differences: key mappings, the text locale and block model access. Reached
 * through {@code ClientServices.CLIENT}.
 */
public interface IClientHelper {

    /**
     * Creates one of the mod's key mappings (registered by the loader project). NeoForge makes it an
     * in-game key and, with {@code controlModifier}, binds the Ctrl key modifier to it; Fabric has
     * no key modifiers, see {@link #isControlModifierSatisfied()}.
     *
     * @param keyCode a GLFW key code, or {@code InputConstants.UNKNOWN.getValue()} for unbound
     */
    KeyMapping createKeyMapping(String name, int keyCode, boolean controlModifier, String category);

    /**
     * Whether the Ctrl requirement of a {@code controlModifier} key mapping is met. Fabric checks
     * {@code Screen.hasControlDown()}; on NeoForge the key modifier already is part of the mapping, so
     * this is always true.
     */
    boolean isControlModifierSatisfied();

    /** The key or button the mapping is currently bound to. */
    InputConstants.Key getBoundKey(KeyMapping keyMapping);

    /** Whether a key press in a screen triggers the mapping (Fabric: vanilla match; NeoForge: bound key code). */
    boolean matchesKey(KeyMapping keyMapping, int keyCode, int scanCode);

    /** Like {@link #matchesKey}, but NeoForge also checks the mapping's conflict context and modifier. */
    boolean isActiveAndMatches(KeyMapping keyMapping, int keyCode, int scanCode);

    /** Locale for line breaking of tooltip text. */
    Locale getLocale();

    /**
     * The parts of a block model, as the loader provides them to a renderer drawing into {@code renderType} without
     * level context (NeoForge asks the model with an empty level, Forge with empty model data and the render type).
     */
    List<BlockModelPart> collectModelParts(BlockStateModel model, BlockState state, RandomSource random, RenderType renderType);

    /** Puts one tinted quad; NeoForge also multiplies the quad's own vertex colours in. */
    void putQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay);
}
