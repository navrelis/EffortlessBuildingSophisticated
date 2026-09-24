package sophisticated.building.neoforge.platform;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import sophisticated.building.platform.services.IClientHelper;

import java.util.List;
import java.util.Locale;

public final class NeoForgeClientHelper implements IClientHelper {

    @Override
    public KeyMapping createKeyMapping(String name, int keyCode, boolean controlModifier, String category) {
        InputConstants.Key key = keyCode == InputConstants.UNKNOWN.getValue() ? InputConstants.UNKNOWN : InputConstants.getKey(keyCode, 0);
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
    public boolean matchesKey(KeyMapping keyMapping, int keyCode, int scanCode) {
        return keyCode == keyMapping.getKey().getValue();
    }

    @Override
    public boolean isActiveAndMatches(KeyMapping keyMapping, int keyCode, int scanCode) {
        return keyMapping.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode));
    }

    @Override
    public Locale getLocale() {
        return Minecraft.getInstance().getLocale();
    }

    @Override
    public List<BakedQuad> getModelQuads(BakedModel model, BlockState state, Direction side, RandomSource random, RenderType renderType) {
        return model.getQuads(state, side, random, ModelData.EMPTY, renderType);
    }

    @Override
    public void putQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
        consumer.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay, true);
    }
}
