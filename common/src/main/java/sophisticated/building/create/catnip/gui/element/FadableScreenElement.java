package sophisticated.building.create.catnip.gui.element;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import sophisticated.building.gui.ScreenElement;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.gui.element.FadableScreenElement}, MIT License, Copyright (c) 2022
 * The Create Team, see LICENSE_Ponder.txt).
 */
@FunctionalInterface
public interface FadableScreenElement extends ScreenElement {

	@Override
	default void render(GuiGraphicsExtractor graphics, int x, int y) {
		render(graphics, x, y, 1f);
	}

	void render(GuiGraphicsExtractor graphics, int x, int y, float alpha);

}
