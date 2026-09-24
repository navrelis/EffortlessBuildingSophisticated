package sophisticated.building.forge.platform;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import sophisticated.building.platform.services.IClientHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ForgeClientHelper implements IClientHelper {

    /** Forge 61 registers no key mapping categories itself: registered with vanilla at once (as on Fabric). */
    @Override
    public KeyMapping.Category createKeyCategory(Identifier id) {
        return KeyMapping.Category.register(id);
    }

    @Override
    public KeyMapping createKeyMapping(String name, int keyCode, boolean controlModifier, KeyMapping.Category category) {
        InputConstants.Key key = keyCode == InputConstants.UNKNOWN.getValue() ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        if (controlModifier) {
            return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.CONTROL, key, category, 0);
        }
        return new KeyMapping(name, KeyConflictContext.IN_GAME, key, category, 0);
    }

    @Override
    public boolean isControlModifierSatisfied() {
        return true;
    }

    @Override
    public InputConstants.Key getBoundKey(KeyMapping keyMapping) {
        return keyMapping.getKey();
    }

    @Override
    public boolean matchesKey(KeyMapping keyMapping, KeyEvent event) {
        return event.key() == keyMapping.getKey().getValue();
    }

    @Override
    public boolean isActiveAndMatches(KeyMapping keyMapping, KeyEvent event) {
        return keyMapping.isActiveAndMatches(InputConstants.getKey(event));
    }

    @Override
    public Locale getLocale() {
        return Minecraft.getInstance().getLanguageManager().getJavaLocale();
    }

    @Override
    public List<BlockStateModelPart> collectModelParts(BlockStateModel model, BlockState state, RandomSource random) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(random, parts, ModelData.EMPTY);
        return parts;
    }
}
