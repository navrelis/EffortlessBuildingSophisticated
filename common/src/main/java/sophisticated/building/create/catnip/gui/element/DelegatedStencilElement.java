package sophisticated.building.create.catnip.gui.element;

import sophisticated.building.create.catnip.gui.UIRenderHelper;
import sophisticated.building.create.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.annotation.Nullable;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.gui.element.DelegatedStencilElement} and
 * {@code StencilElement}, MIT License, Copyright (c) 2022 The Create Team, see LICENSE_Ponder.txt). Catnip rendered the
 * stencil (an icon) into the GL stencil buffer and then any element (a gradient) through it; Minecraft 1.21.5 has no
 * stencil state, so the stencil is a texture region and the element a gradient, drawn together by
 * {@link UIRenderHelper#stencilledGradient}.
 */
public class DelegatedStencilElement extends AbstractRenderElement {

	/** The gradient shown through the stencil, in element coordinates (the element is {@code width} x {@code height}). */
	@FunctionalInterface
	public interface GradientRenderer {
		UIRenderHelper.Gradient gradient(int width, int height, float alpha);
	}

	protected static final GradientRenderer DEFAULT_ELEMENT = (width, height, alpha) -> new UIRenderHelper.Gradient(0, -3, 5, width + 6, new Color(0xff_10dd10).scaleAlpha(alpha), new Color(0xff_1010dd).scaleAlpha(alpha));

	@Nullable
	protected UIRenderHelper.TextureRegion stencil;
	protected GradientRenderer element = DEFAULT_ELEMENT;

	public <T extends DelegatedStencilElement> T withStencil(UIRenderHelper.TextureRegion stencil) {
		this.stencil = stencil;
		//noinspection unchecked
		return (T) this;
	}

	public <T extends DelegatedStencilElement> T withElementRenderer(GradientRenderer renderer) {
		element = renderer;
		//noinspection unchecked
		return (T) this;
	}

	@Override
	public void render(GuiGraphicsExtractor graphics) {
		if (stencil == null)
			return;

		graphics.pose().pushMatrix();
		graphics.pose().translate(getX(), getY());
		UIRenderHelper.stencilledGradient(graphics, stencil, element.gradient(width, height, alpha));
		graphics.pose().popMatrix();
	}

}
