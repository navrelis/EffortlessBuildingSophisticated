package sophisticated.building.fabric.platform;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import sophisticated.building.platform.services.IClientHelper;

import java.util.List;
import java.util.Locale;

public final class FabricClientHelper implements IClientHelper {

    @Override
    public KeyMapping.Category createKeyCategory(Identifier id) {
        return KeyMapping.Category.register(id);
    }

    @Override
    public KeyMapping createKeyMapping(String name, int keyCode, boolean controlModifier, KeyMapping.Category category) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, keyCode, category);
    }

    @Override
    public boolean isControlModifierSatisfied() {
        return Minecraft.getInstance().hasControlDown();
    }

    @Override
    public InputConstants.Key getBoundKey(KeyMapping keyMapping) {
        return KeyBindingHelper.getBoundKeyOf(keyMapping);
    }

    @Override
    public boolean matchesKey(KeyMapping keyMapping, KeyEvent event) {
        return keyMapping.matches(event);
    }

    @Override
    public boolean isActiveAndMatches(KeyMapping keyMapping, KeyEvent event) {
        return keyMapping.matches(event);
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
