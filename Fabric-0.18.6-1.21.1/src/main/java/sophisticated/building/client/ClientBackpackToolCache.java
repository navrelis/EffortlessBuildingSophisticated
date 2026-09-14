package sophisticated.building.client;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Client-side cache of the tools currently sitting in an enabled Tool Swapper / Advanced Tool
 * Swapper upgrade in one of the player's backpacks, kept in sync by
 * {@code BackpackToolsPacket} (T-S4). The client cannot reliably read a backpack's upgrade
 * inventory itself (see 08_SURVIVAL_BREAKING_ANALYSIS.md D6), so this is the only source of
 * backpack tool candidates on the client.
 */
public class ClientBackpackToolCache {

	private static final AtomicReference<List<ItemStack>> TOOLS = new AtomicReference<>(List.of());

	private ClientBackpackToolCache() {
	}

	public static void set(List<ItemStack> tools) {
		List<ItemStack> copies = new ArrayList<>(tools.size());
		for (ItemStack tool : tools) {
			copies.add(tool.copy());
		}
		TOOLS.set(Collections.unmodifiableList(copies));
	}

	public static List<ItemStack> snapshot() {
		List<ItemStack> tools = TOOLS.get();
		List<ItemStack> copies = new ArrayList<>(tools.size());
		for (ItemStack tool : tools) {
			copies.add(tool.copy());
		}
		return copies;
	}

	public static void clear() {
		TOOLS.set(List.of());
	}
}
