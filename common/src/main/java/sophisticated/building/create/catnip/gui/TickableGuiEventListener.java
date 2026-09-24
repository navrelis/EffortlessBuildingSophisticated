package sophisticated.building.create.catnip.gui;

import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.gui.TickableGuiEventListener}, MIT License, Copyright (c) 2022
 * The Create Team, see LICENSE_Ponder.txt).
 */
public interface TickableGuiEventListener extends GuiEventListener {
	void tick();
}
