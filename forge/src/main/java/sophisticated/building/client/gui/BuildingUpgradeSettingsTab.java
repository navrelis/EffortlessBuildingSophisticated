package sophisticated.building.client.gui;

import com.google.common.collect.ImmutableMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.Dimension;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.GuiHelper;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.Position;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.UV;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.Label;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.ToggleButton;
import sophisticated.building.gui.BuildingUpgradeContainer;
import sophisticated.building.item.upgrade.BuildingUpgradeWrapper;

import static net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.ButtonDefinitions.createToggleButtonDefinition;

public class BuildingUpgradeSettingsTab extends UpgradeSettingsTab<BuildingUpgradeContainer> {

    // Sophisticated Backpacks 1.16.3 has no ButtonDefinitions.getBooleanStateData and takes the tooltip as a
    // translation key
    private static final ButtonDefinition.Toggle<Boolean> ENABLED_BUTTON = createToggleButtonDefinition(
            ImmutableMap.of(
                    true, GuiHelper.getButtonStateData(new UV(0, 0), "sophisticatedbuilding.gui.upgrade.enabled", Dimension.SQUARE_16, new Position(1, 1)),
                    false, GuiHelper.getButtonStateData(new UV(16, 0), "sophisticatedbuilding.gui.upgrade.disabled", Dimension.SQUARE_16, new Position(1, 1))
            ));

    public BuildingUpgradeSettingsTab(BuildingUpgradeContainer upgradeContainer, Position position, BackpackScreen screen, Component tabLabel, Component closedTooltip) {
        super(upgradeContainer, position, screen, tabLabel, closedTooltip);

        addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), ENABLED_BUTTON,
                button -> getContainer().setEnabled(!getContainer().isEnabled()),
                () -> getContainer().isEnabled()));

        BuildingUpgradeWrapper wrapper = getContainer().getUpgradeWrapper();
        addHideableChild(new Label(new Position(x + 24, y + 27),
                new TranslatableComponent("sophisticatedbuilding.gui.building_upgrade.info", wrapper.getTier(), wrapper.getMaxBlocks())));
    }

    @Override
    protected void moveSlotsToTab() {
        // No slots to move
    }
}
