package sophisticated.building.forge.platform;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import sophisticated.building.platform.services.IClientHelper;

import java.util.List;
import java.util.Locale;

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
        return Minecraft.getInstance().getLanguageManager().getJavaLocale();
    }

    @Override
    public List<BlockModelPart> collectModelParts(BlockStateModel model, BlockState state, RandomSource random, RenderType renderType) {
        return model.collectParts(random, ModelData.EMPTY, renderType);
    }

    @Override
    public void putQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
        consumer.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay, true);
    }
}
