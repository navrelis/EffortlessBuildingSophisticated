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
 * {@link SmokeBackpackScreens} against the GUI API of Sophisticated Backpacks 1.16.5 (before Sophisticated Core: the
 * classes live in {@code net.p3pp3rf1y.sophisticatedbackpacks.client.gui}): the backpack screen's upgrade settings tab
 * control holds one tab per upgrade; a tab's first button is its icon (opens and closes it), the mod's
 * {@link BuildingUpgradeSettingsTab} adds the enable toggle.
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
        return screen instanceof BackpackScreen
                && openTab(((BackpackScreen) screen).getUpgradeSettingsControl()).filter(tab -> tab instanceof BuildingUpgradeSettingsTab).isPresent();
    }

    @Nullable
    @Override
    public Point buildingTabToggle(Screen screen) {
        return buildingTab(screen).flatMap(tab -> child(tab, ToggleButton.class, true)).map(SophisticatedBackpacksScreens::center).orElse(null);
    }

    private static Optional<BuildingUpgradeSettingsTab> buildingTab(Screen screen) {
        if (!(screen instanceof BackpackScreen)) return Optional.empty();
        for (GuiEventListener child : ((BackpackScreen) screen).getUpgradeSettingsControl().children()) {
            if (child instanceof BuildingUpgradeSettingsTab) return Optional.of((BuildingUpgradeSettingsTab) child);
        }
        return Optional.empty();
    }

    /** SettingsTabControl#getOpenTab is protected in Sophisticated Backpacks 1.16.5 (public in Sophisticated Core). */
    private static Optional<?> openTab(SettingsTabControl<?, ?> control) {
        try {
            Method method = SettingsTabControl.class.getDeclaredMethod("getOpenTab");
            method.setAccessible(true);
            return (Optional<?>) method.invoke(control);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("SettingsTabControl has no getOpenTab()", e);
        }
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
