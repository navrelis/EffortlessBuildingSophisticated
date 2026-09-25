package sophisticated.building.forge.platform;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import sophisticated.building.platform.services.IClientHelper;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class ForgeClientHelper implements IClientHelper {

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
        return Minecraft.getInstance().getLanguageManager().getSelected().getJavaLocale();
    }

    @Override
    public List<BakedQuad> getModelQuads(BakedModel model, BlockState state, Direction side, Random random, RenderType renderType) {
        // Forge 1.16.3 tells multi-layer models the layer being drawn through this thread's render type
        ForgeHooksClient.setRenderLayer(renderType);
        try {
            return model.getQuads(state, side, random, EmptyModelData.INSTANCE);
        } finally {
            ForgeHooksClient.setRenderLayer(null);
        }
    }

    @Override
    public void putQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
        consumer.addVertexData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay, true);
    }
}
