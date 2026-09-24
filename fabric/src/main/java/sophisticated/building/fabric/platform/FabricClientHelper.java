package sophisticated.building.fabric.platform;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
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
    public List<BakedQuad> getModelQuads(BakedModel model, BlockState state, Direction side, RandomSource random, RenderType renderType) {
        return model.getQuads(state, side, random);
    }

    @Override
    public void putQuad(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
        // Vanilla putBulkData (1.20.4) always writes alpha 1: the same loop, with the given alpha.
        int[] vertices = quad.getVertices();
        Vec3i direction = quad.getDirection().getNormal();
        Matrix4f matrix = pose.pose();
        Vector3f normal = pose.normal().transform(new Vector3f(direction.getX(), direction.getY(), direction.getZ()));
        int vertexSize = DefaultVertexFormat.BLOCK.getIntegerSize();
        for (int vertex = 0; vertex < vertices.length / vertexSize; vertex++) {
            int offset = vertex * vertexSize;
            Vector4f position = matrix.transform(new Vector4f(Float.intBitsToFloat(vertices[offset]),
                    Float.intBitsToFloat(vertices[offset + 1]), Float.intBitsToFloat(vertices[offset + 2]), 1.0F));
            consumer.vertex(position.x(), position.y(), position.z(), red, green, blue, alpha,
                    Float.intBitsToFloat(vertices[offset + 4]), Float.intBitsToFloat(vertices[offset + 5]),
                    packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
        }
    }
}
