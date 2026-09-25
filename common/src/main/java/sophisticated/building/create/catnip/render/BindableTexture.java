package sophisticated.building.create.catnip.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.render.BindableTexture}, MIT License, Copyright (c) 2022
 * The Create Team, see LICENSE_Ponder.txt).
 */
public interface BindableTexture {

	default void bind() {
		Minecraft.getInstance().getTextureManager().bind(getLocation());
	}

	ResourceLocation getLocation();

}
