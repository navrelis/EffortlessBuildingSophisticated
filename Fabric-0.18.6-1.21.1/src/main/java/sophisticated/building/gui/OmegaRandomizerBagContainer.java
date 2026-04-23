package sophisticated.building.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.inventory.IItemHandler;
import sophisticated.building.inventory.ItemStackHandler;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.item.OmegaRandomizerBagItem;

public class OmegaRandomizerBagContainer extends AbstractContainerMenu {

	private static final int INV_START = OmegaRandomizerBagItem.INV_SIZE,
			INV_END = INV_START + 26,
			HOTBAR_START = INV_END + 1,
			HOTBAR_END = HOTBAR_START + 8;
	private final IItemHandler bagInventory;

	public OmegaRandomizerBagContainer(MenuType<?> type, int id){
		super(type, id);
		bagInventory = null;
	}

	//Client
	public OmegaRandomizerBagContainer(int id, Inventory playerInventory, FriendlyByteBuf packetBuffer) {
		this(id, playerInventory);
	}

	//Server?
	public OmegaRandomizerBagContainer(int containerId, Inventory playerInventory) {
		this(containerId, playerInventory, new ItemStackHandler(OmegaRandomizerBagItem.INV_SIZE));
	}

	public OmegaRandomizerBagContainer(int containerId, Inventory playerInventory, IItemHandler inventory) {
		super(SophisticatedBuilding.OMEGA_RANDOMIZER_BAG_CONTAINER.get(), containerId);
		bagInventory = inventory;

		// 6 rows of 9 slots (54 slots like a double chest) - TemplateSlot restricts to 1 item per slot and 1 slot per block type
		for (int y = 0; y < 6; ++y) {
			for (int x = 0; x < 9; ++x) {
				addSlot(new TemplateSlot(bagInventory, x + y * 9, 8 + x * 18, 18 + y * 18, OmegaRandomizerBagItem.INV_SIZE));
			}
		}

		// add player inventory slots (starts at y=139, after 6 rows + gap)
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 139 + i * 18));
			}
		}

		// add hotbar slots
		for (int i = 0; i < 9; ++i) {
			addSlot(new Slot(playerInventory, i, 8 + i * 18, 197));
		}
	}

    @Override
	public boolean stillValid(Player playerIn) {
		return playerIn.getMainHandItem().getItem() instanceof OmegaRandomizerBagItem
				|| playerIn.getOffhandItem().getItem() instanceof OmegaRandomizerBagItem;
	}

	@Override
	public Slot getSlot(int parSlotIndex) {
		if (parSlotIndex < 0 || parSlotIndex >= slots.size()) {
			return null;
		}
		return super.getSlot(parSlotIndex);
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int slotIndex) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);

		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();

			// If item is in our custom inventory
			if (slotIndex < INV_START) {
				// try to place in player inventory / action bar
				if (!this.moveItemStackTo(itemstack1, INV_START, HOTBAR_END + 1, true)) {
					return ItemStack.EMPTY;
				}

				slot.onQuickCraft(itemstack1, itemstack);
			}
			// Item is in inventory / hotbar, try to place in custom inventory
			else {
				if (slotIndex >= INV_START) {
					// place in custom inventory
					if (!this.moveItemStackTo(itemstack1, 0, INV_START, false)) {
						return ItemStack.EMPTY;
					}
				}
			}

			if (itemstack1.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (itemstack1.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(playerIn, itemstack1);
		}

		return itemstack;
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		if (clickTypeIn == ClickType.SWAP) {
			ItemStack stack = player.getInventory().getItem(dragType);
			if (!stack.isEmpty() && stack.getItem() instanceof OmegaRandomizerBagItem) {
				return;
			}
		}

		if (slotId >= 0 && slotId < slots.size()) {
			Slot slot = slots.get(slotId);
			ItemStack heldStack = player.containerMenu.getCarried();

			// Prevent placing randomizer bags inside themselves
			if (!heldStack.isEmpty() && heldStack.getItem() instanceof OmegaRandomizerBagItem) {
				if (slot.container != player.getInventory()) {
					return;
				}
			}
		}

		super.clicked(slotId, dragType, clickTypeIn, player);
	}
}
