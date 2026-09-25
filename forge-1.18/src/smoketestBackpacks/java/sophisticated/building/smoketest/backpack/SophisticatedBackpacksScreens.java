package sophisticated.building.smoketest.backpack;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.SettingsTabControl;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.BackpackWidget;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.ButtonBase;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.ToggleButton;
import sophisticated.building.client.gui.BuildingUpgradeSettingsTab;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * {@link SmokeBackpackScreens} against the Sophisticated Backpacks 1.18 GUI API. This copy is the one of forge-1.18:
 * Sophisticated Backpacks 1.18 has no Sophisticated Core, the storage screen is {@link BackpackScreen}, the widgets
 * derive from {@link BackpackWidget}, and {@code SettingsTabControl#getOpenTab()} is protected (read by reflection).
 * The upgrade settings tab control holds one tab per upgrade; a tab's first button is its icon (opens and closes it),
 * the mod's {@link BuildingUpgradeSettingsTab} adds the enable toggle.
 */
public final class SophisticatedBackpacksScreens implements SmokeBackpackScreens {

    @Override
    public boolean isStorageScreen(@Nullable Screen screen) {
        return screen instanceof BackpackScreen;
    }

    @Nullable
    @Override
    public Point buildingTabIcon(Screen screen) {
        return buildingTab(screen).flatMap(tab -> child(tab, ButtonBase.class, false)).map(SophisticatedBackpacksScreens::center).orElse(null);
    }

    @Override
    public boolean isBuildingTabOpen(Screen screen) {
        return screen instanceof BackpackScreen backpack && openTab(backpack.getUpgradeSettingsControl()).filter(tab -> tab instanceof BuildingUpgradeSettingsTab).isPresent();
    }

    @Nullable
    @Override
    public Point buildingTabToggle(Screen screen) {
        return buildingTab(screen).flatMap(tab -> child(tab, ToggleButton.class, true)).map(SophisticatedBackpacksScreens::center).orElse(null);
    }

    private static Optional<?> openTab(SettingsTabControl<?, ?> control) {
        try {
            Method getOpenTab = SettingsTabControl.class.getDeclaredMethod("getOpenTab");
            getOpenTab.setAccessible(true);
            return (Optional<?>) getOpenTab.invoke(control);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("SettingsTabControl#getOpenTab failed", e);
        }
    }

    private static Optional<BuildingUpgradeSettingsTab> buildingTab(Screen screen) {
        if (!(screen instanceof BackpackScreen backpack)) return Optional.empty();
        for (GuiEventListener child : backpack.getUpgradeSettingsControl().children()) {
            if (child instanceof BuildingUpgradeSettingsTab tab) return Optional.of(tab);
        }
        return Optional.empty();
    }

    /** The first child of the type; toggles only when asked for (the tab's icon is a plain button, the toggle a ToggleButton). */
    private static Optional<BackpackWidget> child(BuildingUpgradeSettingsTab tab, Class<? extends ButtonBase> type, boolean toggle) {
        for (GuiEventListener child : tab.children()) {
            if (type.isInstance(child) && (toggle || !(child instanceof ToggleButton<?>))) return Optional.of((BackpackWidget) child);
        }
        return Optional.empty();
    }

    private static Point center(BackpackWidget widget) {
        return new Point(widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0);
    }
}
