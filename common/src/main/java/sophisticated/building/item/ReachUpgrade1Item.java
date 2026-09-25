package sophisticated.building.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import sophisticated.building.CommonConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.attachment.AttachmentHandler;
import sophisticated.building.attachment.PowerLevel;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReachUpgrade1Item extends Item {

	public ReachUpgrade1Item() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		PowerLevel powerLevel = AttachmentHandler.getOrCreatePowerLevel(player);
		if (powerLevel != null) {
			int currentLevel = powerLevel.getPowerLevel();
			if (currentLevel == 0) {
				if (!world.isClientSide) {
					powerLevel.increasePowerLevel();
					AttachmentHandler.setPowerLevel(player, powerLevel);
					SophisticatedBuilding.log(player, Component.translatable("sophisticatedbuilding.message.power_level_upgraded", powerLevel.getPowerLevel()));

					stack.shrink(1);

					world.playSound((Player) null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);

					AttachmentHandler.syncToClient(player);
				}
				return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
			} else if (currentLevel > 0) {
				if (!world.isClientSide && hand == InteractionHand.MAIN_HAND) {
					SophisticatedBuilding.log(player, Component.translatable("sophisticatedbuilding.message.reach_upgrade_already_used", powerLevel.getPowerLevel()));

					world.playSound((Player) null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1f, 1f);
				}
			}
		}

		return InteractionResultHolder.fail(player.getItemInHand(hand));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
		tooltip.add(Component.translatable("item.sophisticatedbuilding.reach_upgrade.tooltip", CommonConfig.reach.level1.get()));
	}

}
