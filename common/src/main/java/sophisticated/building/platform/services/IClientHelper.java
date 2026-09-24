package sophisticated.building.platform.services;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;
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
     * Creates the category of the mod's key mappings (Minecraft 1.21.9+: a registered {@code KeyMapping.Category}
     * instead of a translation key). Fabric registers it with vanilla at once; NeoForge registers it in its key
     * mapping event.
     */
    KeyMapping.Category createKeyCategory(Identifier id);

    /**
     * Creates one of the mod's key mappings (registered by the loader project). NeoForge makes it an
     * in-game key and, with {@code controlModifier}, binds the Ctrl key modifier to it; Fabric has
     * no key modifiers, see {@link #isControlModifierSatisfied()}.
     *
     * @param keyCode a GLFW key code, or {@code InputConstants.UNKNOWN.getValue()} for unbound
     */
    KeyMapping createKeyMapping(String name, int keyCode, boolean controlModifier, KeyMapping.Category category);

    /**
     * Whether the Ctrl requirement of a {@code controlModifier} key mapping is met. Fabric checks
     * {@code Minecraft#hasControlDown()}; on NeoForge the key modifier already is part of the mapping, so
     * this is always true.
     */
    boolean isControlModifierSatisfied();

    /** The key or button the mapping is currently bound to. */
    InputConstants.Key getBoundKey(KeyMapping keyMapping);

    /** Whether a key press in a screen triggers the mapping (Fabric: vanilla match; NeoForge: bound key code). */
    boolean matchesKey(KeyMapping keyMapping, KeyEvent event);

    /** Like {@link #matchesKey}, but NeoForge also checks the mapping's conflict context and modifier. */
    boolean isActiveAndMatches(KeyMapping keyMapping, KeyEvent event);

    /** Locale for line breaking of tooltip text. */
    Locale getLocale();

    /**
     * All parts of a block model, as the loader provides them to a renderer without level context (NeoForge asks the
     * model with an empty level, Forge with empty model data and no chunk layer: since Minecraft 1.21.6 the chunk
     * layers are no render types, and the ghost blocks draw every part translucent anyway).
     */
    List<BlockStateModelPart> collectModelParts(BlockStateModel model, BlockState state, RandomSource random);
}
