package sophisticated.building.integration;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import sophisticated.building.client.gui.BuildingUpgradeSettingsTab;
import sophisticated.building.gui.BuildingUpgradeContainer;

public class SophisticatedBackpacksClientIntegration {

    public static void registerUpgradeTab() {
        UpgradeGuiManager.registerTab(
            SophisticatedBackpacksIntegration.getContainerType(),
            (BuildingUpgradeContainer c, Position p, StorageScreenBase<?> s) -> 
                new BuildingUpgradeSettingsTab(c, p, s, 
                    Component.translatable("sophisticatedbuilding.screen.modifier_settings"), 
                    Component.empty())
        );
    }
}
