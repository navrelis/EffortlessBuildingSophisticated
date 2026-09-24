package sophisticated.building.fabric.platform;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.platform.services.IClientHelper;

import java.util.List;
import java.util.Locale;

public final class FabricClientHelper implements IClientHelper {

    @Override
    public KeyMapping createKeyMapping(String name, int keyCode, boolean controlModifier, String category) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, keyCode, category);
    }

    @Override
    public boolean isControlModifierSatisfied() {
        return Screen.hasControlDown();
    }

    @Override
    public InputConstants.Key getBoundKey(KeyMapping keyMapping) {
        return KeyBindingHelper.getBoundKeyOf(keyMapping);
    }

    @Override
    public boolean matchesKey(KeyMapping keyMapping, int keyCode, int scanCode) {
        return keyMapping.matches(keyCode, scanCode);
    }

    @Override
    public boolean isActiveAndMatches(KeyMapping keyMapping, int keyCode, int scanCode) {
        return keyMapping.matches(keyCode, scanCode);
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public List<BlockModelPart> collectModelParts(BlockStateModel model, BlockState state, RandomSource random) {
        return model.collectParts(random);
    }

    @Override
    public void putQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
        consumer.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay);
    }
}
