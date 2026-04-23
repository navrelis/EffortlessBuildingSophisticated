package sophisticated.building.gui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;
import sophisticated.building.item.upgrade.BuildingUpgradeWrapper;

public class BuildingUpgradeContainer extends UpgradeContainerBase<BuildingUpgradeWrapper, BuildingUpgradeContainer> {
    private static final String DATA_ENABLED = "enabled";

    public BuildingUpgradeContainer(Player player, int upgradeContainerId, BuildingUpgradeWrapper upgradeWrapper, UpgradeContainerType<BuildingUpgradeWrapper, BuildingUpgradeContainer> type) {
        super(player, upgradeContainerId, upgradeWrapper, type);
    }

    @Override
    public void handlePacket(CompoundTag data) {
        if (data.contains(DATA_ENABLED)) {
            setEnabled(data.getBoolean(DATA_ENABLED));
        }
    }

    public void setEnabled(boolean enabled) {
        upgradeWrapper.setEnabled(enabled);
        sendBooleanToServer(DATA_ENABLED, enabled);
    }

    public boolean isEnabled() {
        return upgradeWrapper.isEnabled();
    }
}
