package sophisticated.building.buildmodifier;

import sophisticated.building.utilities.NBTUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import sophisticated.building.network.message.ModifierSettingsPacket;
import sophisticated.building.utilities.BlockSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BuildModifiers {
	private List<BaseModifier> modifierSettingsList = new ArrayList<>();

	public List<BaseModifier> getModifierSettingsList() {
		return Collections.unmodifiableList(modifierSettingsList);
	}

	public void addModifierSettings(BaseModifier modifierSettings) {
		modifierSettingsList.add(modifierSettings);
	}

	public void removeModifierSettings(BaseModifier modifierSettings) {
		modifierSettingsList.remove(modifierSettings);
	}

	public void removeModifierSettings(int index) {
		modifierSettingsList.remove(index);
	}

	public void moveUp(BaseModifier modifierSettings) {
		int index = modifierSettingsList.indexOf(modifierSettings);
		if (index == 0) return;

		Collections.swap(modifierSettingsList, index, index - 1);
	}

	public void moveDown(BaseModifier modifierSettings) {
		int index = modifierSettingsList.indexOf(modifierSettings);
		if (index == modifierSettingsList.size() - 1) return;

		Collections.swap(modifierSettingsList, index, index + 1);
	}

	public void setFirst(BaseModifier modifierSettings) {
		int index = modifierSettingsList.indexOf(modifierSettings);
		if (index == 0) return;

		modifierSettingsList.remove(index);
		modifierSettingsList.add(0, modifierSettings);
	}

	public void setLast(BaseModifier modifierSettings) {
		int index = modifierSettingsList.indexOf(modifierSettings);
		if (index == modifierSettingsList.size() - 1) return;

		modifierSettingsList.remove(index);
		modifierSettingsList.add(modifierSettings);
	}

	public void clearAllModifierSettings() {
		modifierSettingsList.clear();
	}

	public void findCoordinates(BlockSet blocks, Player player) {
		for (BaseModifier modifierSettings : modifierSettingsList) {
			modifierSettings.findCoordinates(blocks, player);
		}
	}

	public void onPowerLevelChanged(int powerLevel) {
		for (BaseModifier modifierSettings : modifierSettingsList) {
			modifierSettings.onPowerLevelChanged(powerLevel);
		}
	}

	public CompoundTag serializeNBT() {
		var compoundTag = new CompoundTag();
		compoundTag.put("modifierSettingsList", NBTUtils.writeCompoundList(modifierSettingsList, BaseModifier::serializeNBT));
		return compoundTag;
	}

	public void deserializeNBT(CompoundTag compoundTag) {
		var listTag = compoundTag.getList("modifierSettingsList", Tag.TAG_COMPOUND);
		modifierSettingsList = NBTUtils.readCompoundList(listTag, tag -> {
			var modifier = createModifier(tag.getString("type"));
			modifier.deserializeNBT(tag);
			return modifier;
		});
	}

	public void save() {
		PacketDistributor.sendToServer(new ModifierSettingsPacket(serializeNBT()));

		//Save locally as well?
//		var listTag = NBTHelper.writeCompoundList(modifierSettingsList, BaseModifier::serializeNBT);
//		player.getPersistentData().put(DATA_KEY, listTag);
	}

	private BaseModifier createModifier(String type) {
		switch (type) {
			case "Mirror": return new Mirror();
			case "Array": return new Array();
			case "RadialMirror": return new RadialMirror();
			default: throw new IllegalArgumentException("Unknown modifier type: " + type);
		}
	}
}
