package sophisticated.building.create.foundation.item;

import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TagDependentIngredientItem extends Item {

	private TagKey<Item> tag;

	public TagDependentIngredientItem(Properties properties, TagKey<Item> tag) {
		super(properties);
		this.tag = tag;
	}

	public boolean shouldHide() {
		return Registry.ITEM.getTag(tag).isEmpty() || Registry.ITEM.getTag(tag).get().size() == 0;
	}

}
