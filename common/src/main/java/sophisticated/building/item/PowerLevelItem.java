package sophisticated.building.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.attachment.PowerLevel;
import sophisticated.building.create.foundation.item.TooltipHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class PowerLevelItem extends Item {
    public PowerLevelItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PowerLevel powerLevel = AttachmentHandler.getOrCreatePowerLevel(player);
        if (powerLevel != null) {
            if (powerLevel.canIncreasePowerLevel()) {
                if (!world.isClientSide()) {
                    powerLevel.increasePowerLevel();
                    AttachmentHandler.setPowerLevel(player, powerLevel);
                    SophisticatedBuilding.log(player, "Upgraded power level to " + powerLevel.getPowerLevel());

                    stack.shrink(1);

                    world.playSound((Player) null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);

                    AttachmentHandler.syncToClient(player);
                }

                return InteractionResult.SUCCESS;
            } else {
                if (!world.isClientSide()) {
                    SophisticatedBuilding.log(player, "Already reached maximum power level!");

                    world.playSound((Player) null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1f, 1f);
                }

                return InteractionResult.FAIL;
            }
        }

        return super.use(world, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        TooltipHelper.cutTextComponent(Component.translatable(getDescriptionId() + ".desc"), ChatFormatting.GRAY, ChatFormatting.GRAY).forEach(tooltip);
        TooltipHelper.cutTextComponent(Component.translatable("key.sophisticatedbuilding.upgrade_power_level"), ChatFormatting.BLUE, ChatFormatting.BLUE).forEach(tooltip);
    }
}
