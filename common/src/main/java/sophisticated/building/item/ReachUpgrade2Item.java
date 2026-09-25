package sophisticated.building.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
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

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReachUpgrade2Item extends Item {

	public ReachUpgrade2Item() {
		super(new Item.Properties().stacksTo(1).tab(SophisticatedBuilding.CREATIVE_TAB));
	}


	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		PowerLevel powerLevel = AttachmentHandler.getOrCreatePowerLevel(player);
		if (powerLevel != null) {
			int currentLevel = powerLevel.getPowerLevel();
			if (currentLevel == 1) {
				if (!world.isClientSide) {
					powerLevel.increasePowerLevel();
					AttachmentHandler.setPowerLevel(player, powerLevel);
					SophisticatedBuilding.log(player, new TranslatableComponent("sophisticatedbuilding.message.power_level_upgraded", powerLevel.getPowerLevel()));

					stack.shrink(1);

					world.playSound((Player) null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1f);

					AttachmentHandler.syncToClient(player);
				}
				return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
			} else if (currentLevel < 1) {
				if (!world.isClientSide) {
					SophisticatedBuilding.log(player, new TranslatableComponent("sophisticatedbuilding.message.reach_upgrade_needs_1"));

					world.playSound((Player) null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1f, 1f);
				}
			} else if (currentLevel > 1) {
				if (!world.isClientSide) {
					SophisticatedBuilding.log(player, new TranslatableComponent("sophisticatedbuilding.message.reach_upgrade_already_used", powerLevel.getPowerLevel()));

					world.playSound((Player) null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 1f, 1f);
				}
			}
		}

		return InteractionResultHolder.fail(player.getItemInHand(hand));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
		tooltip.add(new TranslatableComponent("item.sophisticatedbuilding.reach_upgrade.tooltip", CommonConfig.reach.level2.get()));
		tooltip.add(new TranslatableComponent("item.sophisticatedbuilding.reach_upgrade.tooltip.previous"));
	}
}
