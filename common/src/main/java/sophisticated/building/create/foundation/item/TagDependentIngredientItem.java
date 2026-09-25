package sophisticated.building.create.foundation.item;

import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;

public class TagDependentIngredientItem extends Item {

	// Minecraft 1.17.1 has no TagKey (1.18.2+): the tag object itself holds the bound values
	private Tag<Item> tag;

	public TagDependentIngredientItem(Properties properties, Tag<Item> tag) {
		super(properties);
		this.tag = tag;
	}

	public boolean shouldHide() {
		return tag.getValues().isEmpty();
	}

}
