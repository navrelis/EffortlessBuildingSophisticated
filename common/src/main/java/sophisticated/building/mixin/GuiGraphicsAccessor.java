package sophisticated.building.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Since Minecraft 1.21.6 the GUI is drawn from render states and {@link GuiGraphics} only submits rectangles; free-form
 * quads (radial menu segments, rotated gradients, stencilled icons) are submitted to its render state directly, see
 * {@link sophisticated.building.client.gui.GuiQuads}.
 */
@Mixin(GuiGraphics.class)
public interface GuiGraphicsAccessor {

	@Accessor("guiRenderState")
	GuiRenderState sophisticatedbuilding$getGuiRenderState();
}
