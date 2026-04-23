package sophisticated.building.create.foundation.block.render;

import net.minecraft.resources.ResourceLocation;
import sophisticated.building.create.foundation.block.render.ConnectedTextureBehaviour.CTContext;
import sophisticated.building.create.foundation.block.render.ConnectedTextureBehaviour.ContextRequirement;

public interface CTType {
	ResourceLocation getId();

	int getSheetSize();

	ContextRequirement getContextRequirement();

	int getTextureIndex(CTContext context);
}
