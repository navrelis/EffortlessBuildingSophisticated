package sophisticated.building.client.gui;

import net.minecraft.network.chat.Component;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.GuiHelper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import sophisticated.building.gui.BuildingUpgradeContainer;

import static net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions.createToggleButtonDefinition;
import static net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions.getBooleanStateData;

public class BuildingUpgradeSettingsTab extends UpgradeSettingsTab<BuildingUpgradeContainer> {

	private static final ButtonDefinition.Toggle<Boolean> ENABLED_BUTTON = createToggleButtonDefinition(
			getBooleanStateData(
					GuiHelper.getButtonStateData(new UV(0, 0), Dimension.SQUARE_16, new Position(1, 1), Component.translatable("sophisticatedbuilding.gui.upgrade.enabled")),
					GuiHelper.getButtonStateData(new UV(16, 0), Dimension.SQUARE_16, new Position(1, 1), Component.translatable("sophisticatedbuilding.gui.upgrade.disabled"))
			));

	public BuildingUpgradeSettingsTab(BuildingUpgradeContainer upgradeContainer, Position position, StorageScreenBase<?> screen, Component tabLabel, Component closedTooltip) {
		super(upgradeContainer, position, screen, tabLabel, closedTooltip);

		addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), ENABLED_BUTTON,
				button -> getContainer().setEnabled(!getContainer().isEnabled()),
				() -> getContainer().isEnabled()));
	}

	@Override
	protected void moveSlotsToTab() {
		// No slots to move.
	}
}
