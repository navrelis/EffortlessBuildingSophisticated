package sophisticated.building.neoforge.platform;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import sophisticated.building.platform.services.IClientHelper;

import java.util.List;
import java.util.Locale;

public final class NeoForgeClientHelper implements IClientHelper {

    /** Registered in NeoForge's key mapping event (SophisticatedBuildingNeoForgeClient#registerKeyMappings). */
    @Override
    public KeyMapping.Category createKeyCategory(Identifier id) {
        return new KeyMapping.Category(id);
    }

    @Override
    public KeyMapping createKeyMapping(String name, int keyCode, boolean controlModifier, KeyMapping.Category category) {
        InputConstants.Key key = keyCode == InputConstants.UNKNOWN.getValue() ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        if (controlModifier) {
            return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.CONTROL, key, category);
        }
        return new KeyMapping(name, KeyConflictContext.IN_GAME, key, category);
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
        return Minecraft.getInstance().getLocale();
    }

    @Override
    public List<BlockModelPart> collectModelParts(BlockStateModel model, BlockState state, RandomSource random) {
        return model.collectParts(EmptyBlockAndTintGetter.INSTANCE, BlockPos.ZERO, state, random);
    }

    @Override
    public void putQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
        consumer.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay);
    }
}
