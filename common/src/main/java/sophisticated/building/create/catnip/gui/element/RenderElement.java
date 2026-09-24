package sophisticated.building.create.catnip.gui.element;

import sophisticated.building.client.gui.GuiGraphics;
import sophisticated.building.gui.ScreenElement;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.gui.element.RenderElement}, MIT License, Copyright (c) 2022
 * The Create Team, see LICENSE_Ponder.txt).
 */
public interface RenderElement extends FadableScreenElement {

	static RenderElement of(ScreenElement renderable) {
		return new AbstractRenderElement.SimpleRenderElement(renderable);
	}

	<T extends RenderElement> T at(float x, float y);

	<T extends RenderElement> T at(float x, float y, float z);

	<T extends RenderElement> T withBounds(int width, int height);

	<T extends RenderElement> T withAlpha(float alpha);

	int getWidth();

	int getHeight();

	float getX();

	float getY();

	float getZ();

	void render(GuiGraphics graphics);

	@Override
	default void render(GuiGraphics graphics, int x, int y, float alpha) {
		this.at(x, y).withAlpha(alpha).render(graphics);
	}
}
