package sophisticated.building.smoketest.backpack;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.UpgradeSettingsControl;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.ButtonBase;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.controls.Widget;
import sophisticated.building.client.gui.BuildingUpgradeSettingsTab;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * {@link SmokeBackpackScreens} against the GUI API of Sophisticated Backpacks 1.16.4-1.0.0.94 (the 1.16.3 build, before
 * Sophisticated Core: the classes live in {@code net.p3pp3rf1y.sophisticatedbackpacks.client.gui}, the widget base class
 * is {@code Widget}, and the tabs sit in the screen's {@code UpgradeSettingsControl}, not a {@code SettingsTabControl}):
 * the control holds one tab per upgrade; a tab's first button is its icon (an item button that opens and closes it),
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
        return screen instanceof BackpackScreen
                && openTab(((BackpackScreen) screen).getUpgradeControl()) instanceof BuildingUpgradeSettingsTab;
    }

    @Nullable
    @Override
    public Point buildingTabToggle(Screen screen) {
        return buildingTab(screen).flatMap(tab -> child(tab, ToggleButton.class, true)).map(SophisticatedBackpacksScreens::center).orElse(null);
    }

    private static Optional<BuildingUpgradeSettingsTab> buildingTab(Screen screen) {
        if (!(screen instanceof BackpackScreen)) return Optional.empty();
        for (GuiEventListener child : ((BackpackScreen) screen).getUpgradeControl().children()) {
            if (child instanceof BuildingUpgradeSettingsTab) return Optional.of((BuildingUpgradeSettingsTab) child);
        }
        return Optional.empty();
    }

    /** UpgradeSettingsControl keeps the open tab in a private field (no getter in Sophisticated Backpacks 1.0.0.94). */
    @Nullable
    private static Object openTab(UpgradeSettingsControl control) {
        try {
            Field field = UpgradeSettingsControl.class.getDeclaredField("openTab");
            field.setAccessible(true);
            return field.get(control);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("UpgradeSettingsControl has no openTab field", e);
        }
    }

    /** The first child of the type; toggles only when asked for (the tab's icon is an item button, the toggle a ToggleButton). */
    private static Optional<Widget> child(BuildingUpgradeSettingsTab tab, Class<? extends ButtonBase> type, boolean toggle) {
        for (GuiEventListener child : tab.children()) {
            if (type.isInstance(child) && (toggle || !(child instanceof ToggleButton<?>))) return Optional.of((Widget) child);
        }
        return Optional.empty();
    }

    private static Point center(Widget widget) {
        return new Point(widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0);
    }
}
