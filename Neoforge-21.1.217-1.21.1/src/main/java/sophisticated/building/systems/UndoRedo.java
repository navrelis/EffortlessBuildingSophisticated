package sophisticated.building.systems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.ServerConfig;
import sophisticated.building.utilities.BlockSet;
import sophisticated.building.utilities.FixedStack;
import sophisticated.building.utilities.InventoryHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//Server only
public class UndoRedo {

	public static class UndoSet {
		public final List<BlockSnapshot> blockSnapshots;

		public UndoSet(List<BlockSnapshot> blockSnapshots) {
			this.blockSnapshots = blockSnapshots;
		}
	}

	public final Map<UUID, FixedStack<BlockSet>> undoStacks = new HashMap<>();
	public final Map<UUID, FixedStack<BlockSet>> redoStacks = new HashMap<>();

	public boolean isAllowedToUndo(Player player) {
		return true;
	}

	public void addUndo(Player player, BlockSet blockSet) {
		if (blockSet.isEmpty() || !isAllowedToUndo(player)) return;

		//If no stack exists, make one
		if (!undoStacks.containsKey(player.getUUID())) {
			undoStacks.put(player.getUUID(), new FixedStack<>(new BlockSet[ServerConfig.memory.undoStackSize.get()]));
		}

		undoStacks.get(player.getUUID()).push(blockSet);
	}

	public void addRedo(Player player, BlockSet blockSet) {
		if (blockSet.isEmpty() || !isAllowedToUndo(player)) return;

		//If no stack exists, make one
		if (!redoStacks.containsKey(player.getUUID())) {
			redoStacks.put(player.getUUID(), new FixedStack<>(new BlockSet[ServerConfig.memory.undoStackSize.get()]));
		}

		redoStacks.get(player.getUUID()).push(blockSet);
	}

	public boolean undo(Player player) {
		if (!isAllowedToUndo(player)) {
			SophisticatedBuilding.log(player, ChatFormatting.RED + "You are not allowed to undo.");
			return false;
		}

		if (!undoStacks.containsKey(player.getUUID())) return false;

		FixedStack<BlockSet> undoStack = undoStacks.get(player.getUUID());
		if (undoStack.isEmpty()) return false;

		BlockSet blockSet = undoStack.pop();
		SophisticatedBuilding.SERVER_BLOCK_PLACER.undoBlockSet(player, blockSet);

		return true;
	}

	public boolean redo(Player player) {
		if (!isAllowedToUndo(player)) {
			SophisticatedBuilding.log(player, ChatFormatting.RED + "You are not allowed to undo.");
			return false;
		}

		if (!redoStacks.containsKey(player.getUUID())) return false;

		FixedStack<BlockSet> redoStack = redoStacks.get(player.getUUID());
		if (redoStack.isEmpty()) return false;

		BlockSet blockSet = redoStack.pop();
		SophisticatedBuilding.SERVER_BLOCK_PLACER.applyBlockSet(player, blockSet);

		return true;
	}

	public void clear(Player player) {
		if (undoStacks.containsKey(player.getUUID())) {
			undoStacks.get(player.getUUID()).clear();
		}
		if (redoStacks.containsKey(player.getUUID())) {
			redoStacks.get(player.getUUID()).clear();
		}
	}

	private List<ItemStack> findItemStacksInInventory(Player player, List<BlockState> blockStates) {
		List<ItemStack> itemStacks = new ArrayList<>(blockStates.size());
		for (BlockState blockState : blockStates) {
			itemStacks.add(findItemStackInInventory(player, blockState));
		}
		return itemStacks;
	}

	private ItemStack findItemStackInInventory(Player player, BlockState blockState) {
		ItemStack itemStack = ItemStack.EMPTY;
		if (blockState == null) return itemStack;

		//First try previousBlockStates
		itemStack = InventoryHelper.findItemStackInInventory(player, blockState.getBlock());

		//then anything it drops
		if (itemStack.isEmpty()) {
			//Cannot check drops on clientside because loot tables are server only
			if (!player.level().isClientSide) {
				List<ItemStack> itemsDropped = Block.getDrops(blockState, (ServerLevel) player.level(), BlockPos.ZERO, null);
				for (ItemStack itemStackDropped : itemsDropped) {
					if (itemStackDropped.getItem() instanceof BlockItem) {
						Block block = ((BlockItem) itemStackDropped.getItem()).getBlock();
						itemStack = InventoryHelper.findItemStackInInventory(player, block);
					}
				}
			}
		}

		return itemStack;
	}
}
