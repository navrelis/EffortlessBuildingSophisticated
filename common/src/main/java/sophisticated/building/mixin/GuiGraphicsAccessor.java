package sophisticated.building.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Since Minecraft 1.21.6 the GUI is drawn from render states and {@link GuiGraphicsExtractor} only submits rectangles; free-form
 * quads (radial menu segments, rotated gradients, stencilled icons) are submitted to its render state directly, see
 * {@link sophisticated.building.client.gui.GuiQuads}.
 */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsAccessor {

	@Accessor("guiRenderState")
	GuiRenderState sophisticatedbuilding$getGuiRenderState();
}
