package sophisticated.building;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.ModifierLimits;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModifierLimitsTest {

	private static CompoundTag array(int dx, int count, boolean enabled) {
		CompoundTag tag = new CompoundTag();
		tag.putString("type", "Array");
		tag.putBoolean("enabled", enabled);
		tag.putIntArray("offset", new int[]{dx, 0, 0});
		tag.putInt("count", count);
		return tag;
	}

	private static CompoundTag mirror(String type, int radius) {
		CompoundTag tag = new CompoundTag();
		tag.putString("type", type);
		tag.putBoolean("enabled", true);
		tag.putInt("radius", radius);
		return tag;
	}

	private static CompoundTag modifiers(CompoundTag... entries) {
		ListTag list = new ListTag();
		for (CompoundTag entry : entries) list.add(entry);
		CompoundTag tag = new CompoundTag();
		tag.put(ModifierLimits.LIST_KEY, list);
		return tag;
	}

	@Test
	void capCutsArraysAndMirrors() {
		CompoundTag tag = ModifierLimits.cap(modifiers(array(2, 20, true), mirror("Mirror", 500), mirror("RadialMirror", 10)), 8, 16);
		ListTag list = tag.getList(ModifierLimits.LIST_KEY, 10); // 10 = compound (Tag.TAG_COMPOUND from 1.17 on)
		assertEquals(4, list.getCompound(0).getInt("count"));
		assertEquals(16, list.getCompound(1).getInt("radius"));
		assertEquals(10, list.getCompound(2).getInt("radius"));
	}

	@Test
	void reachSumsTheEnabledModifiers() {
		// array extent 2 * 4 = 8 (capped from 20), mirror 2 * 16, disabled array ignored
		assertEquals(8 + 32, ModifierLimits.reach(modifiers(array(2, 20, true), mirror("Mirror", 500), array(5, 5, false)), 8, 16));
		assertEquals(0, ModifierLimits.reach(new CompoundTag(), 8, 16));
	}
}
