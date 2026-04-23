package sophisticated.building.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import sophisticated.building.gui.GoldenRandomizerBagContainer;

import javax.annotation.Nullable;

public class GoldenRandomizerBagItem extends AbstractRandomizerBagItem{
    public static final int INV_SIZE = 9;

    @Override
    public int getInventorySize() {
        return 9;
    }

    @Override
    public MenuProvider getContainerProvider(ItemStack bag) {
        return new ContainerProvider(bag);
    }

    public static class ContainerProvider implements MenuProvider {

        private final ItemStack bag;

        public ContainerProvider(ItemStack bag) {
            this.bag = bag;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.sophisticatedbuilding.golden_randomizer_bag");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new GoldenRandomizerBagContainer(containerId, playerInventory, ((AbstractRandomizerBagItem)bag.getItem()).getBagInventory(bag));
        }
    }
}
