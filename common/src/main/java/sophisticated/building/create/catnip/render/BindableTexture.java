package sophisticated.building.create.catnip.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;

/**
 * Adapted from Catnip ({@code sophisticated.building.create.catnip.render.BindableTexture}, MIT License, Copyright (c) 2022
 * The Create Team, see LICENSE_Ponder.txt).
 */
public interface BindableTexture {

	default void bind() {
		RenderSystem.setShaderTexture(0, getLocation());
	}

	ResourceLocation getLocation();

}
