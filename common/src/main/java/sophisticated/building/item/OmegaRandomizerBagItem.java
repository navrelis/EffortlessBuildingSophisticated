package sophisticated.building.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import sophisticated.building.gui.OmegaRandomizerBagContainer;

import javax.annotation.Nullable;

public class OmegaRandomizerBagItem extends AbstractRandomizerBagItem {
    public static final int INV_SIZE = 54; // Double chest size
    private static final String WEIGHTS_TAG = "SlotWeights";
    private static final int DEFAULT_WEIGHT = 1;
    private static final int MIN_WEIGHT = 1;
    private static final int MAX_WEIGHT = 90; // Max 90% weight

    @Override
    public int getInventorySize() {
        return 54;
    }

    @Override
    public MenuProvider getContainerProvider(ItemStack bag) {
        return new ContainerProvider(bag);
    }

    /**
     * Get the weight for a specific slot (1-10, default 1)
     */
    public int getSlotWeight(ItemStack bag, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= INV_SIZE) return DEFAULT_WEIGHT;
        
        CustomData customData = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) return DEFAULT_WEIGHT;
        
        var tag = customData.copyTag();
        if (!tag.contains(WEIGHTS_TAG)) return DEFAULT_WEIGHT;
        
        var weightsArray = tag.getIntArray(WEIGHTS_TAG);
        if (weightsArray.length <= slotIndex) return DEFAULT_WEIGHT;
        
        return Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, weightsArray[slotIndex]));
    }

    /**
     * Set the weight for a specific slot
     */
    public void setSlotWeight(ItemStack bag, int slotIndex, int weight) {
        if (slotIndex < 0 || slotIndex >= INV_SIZE) return;
        weight = Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, weight));
        
        CustomData customData = bag.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = customData.copyTag();
        
        int[] weights;
        if (tag.contains(WEIGHTS_TAG)) {
            weights = tag.getIntArray(WEIGHTS_TAG);
            if (weights.length < INV_SIZE) {
                // Expand array
                int[] newWeights = new int[INV_SIZE];
                System.arraycopy(weights, 0, newWeights, 0, weights.length);
                for (int i = weights.length; i < INV_SIZE; i++) {
                    newWeights[i] = DEFAULT_WEIGHT;
                }
                weights = newWeights;
            }
        } else {
            // Create new array
            weights = new int[INV_SIZE];
            for (int i = 0; i < INV_SIZE; i++) {
                weights[i] = DEFAULT_WEIGHT;
            }
        }
        
        weights[slotIndex] = weight;
        tag.putIntArray(WEIGHTS_TAG, weights);
        bag.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Get all slot weights as an array
     */
    public int[] getAllWeights(ItemStack bag) {
        int[] weights = new int[INV_SIZE];
        for (int i = 0; i < INV_SIZE; i++) {
            weights[i] = getSlotWeight(bag, i);
        }
        return weights;
    }

    public static int getMinWeight() {
        return MIN_WEIGHT;
    }

    public static int getMaxWeight() {
        return MAX_WEIGHT;
    }

    public static int getDefaultWeight() {
        return DEFAULT_WEIGHT;
    }

    public static class ContainerProvider implements MenuProvider {

        private final ItemStack bag;

        public ContainerProvider(ItemStack bag) {
            this.bag = bag;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("item.sophisticatedbuilding.omega_randomizer_bag");
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new OmegaRandomizerBagContainer(containerId, playerInventory, ((AbstractRandomizerBagItem)bag.getItem()).getBagInventory(bag));
        }
    }
}
