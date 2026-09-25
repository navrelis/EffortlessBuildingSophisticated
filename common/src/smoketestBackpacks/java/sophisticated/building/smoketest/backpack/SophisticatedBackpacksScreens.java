package sophisticated.building.smoketest.backpack;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsTabControl;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import sophisticated.building.client.gui.BuildingUpgradeSettingsTab;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * {@link SmokeBackpackScreens} against the Sophisticated Core GUI API (Forge 1.19.2 and 1.19, and the Fabric port for
 * 1.19.2): the storage screen's upgrade settings tab control holds one tab per upgrade; a tab's first
 * button is its icon (opens and closes it), the mod's {@link BuildingUpgradeSettingsTab} adds the enable toggle.
 */
public final class SophisticatedBackpacksScreens implements SmokeBackpackScreens {

    @Override
    public boolean isStorageScreen(@Nullable Screen screen) {
        return screen instanceof StorageScreenBase<?>;
    }

    @Nullable
    @Override
    public Point buildingTabIcon(Screen screen) {
        return buildingTab(screen).flatMap(tab -> child(tab, ButtonBase.class, false)).map(SophisticatedBackpacksScreens::center).orElse(null);
    }

    @Override
    public boolean isBuildingTabOpen(Screen screen) {
        return screen instanceof StorageScreenBase<?> storage && openTab(storage.getUpgradeSettingsControl()) instanceof BuildingUpgradeSettingsTab;
    }

    /**
     * The open tab, or null. SettingsTabControl#getOpenTab is public from Sophisticated Core 0.6 on (1.19.2) but
     * protected in Core 0.4 (1.19 and 1.19.1, forge-1.19), so it is called by reflection.
     */
    @Nullable
    private static Object openTab(SettingsTabControl<?, ?> control) {
        try {
            Method getOpenTab = SettingsTabControl.class.getDeclaredMethod("getOpenTab");
            getOpenTab.setAccessible(true);
            return ((Optional<?>) getOpenTab.invoke(control)).orElse(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("SettingsTabControl#getOpenTab failed", e);
        }
    }

    @Nullable
    @Override
    public Point buildingTabToggle(Screen screen) {
        return buildingTab(screen).flatMap(tab -> child(tab, ToggleButton.class, true)).map(SophisticatedBackpacksScreens::center).orElse(null);
    }

    private static Optional<BuildingUpgradeSettingsTab> buildingTab(Screen screen) {
        if (!(screen instanceof StorageScreenBase<?> storage)) return Optional.empty();
        for (GuiEventListener child : storage.getUpgradeSettingsControl().children()) {
            if (child instanceof BuildingUpgradeSettingsTab tab) return Optional.of(tab);
        }
        return Optional.empty();
    }

    /** The first child of the type; toggles only when asked for (the tab's icon is a plain button, the toggle a ToggleButton). */
    private static Optional<WidgetBase> child(BuildingUpgradeSettingsTab tab, Class<? extends ButtonBase> type, boolean toggle) {
        for (GuiEventListener child : tab.children()) {
            if (type.isInstance(child) && (toggle || !(child instanceof ToggleButton<?>))) return Optional.of((WidgetBase) child);
        }
        return Optional.empty();
    }

    private static Point center(WidgetBase widget) {
        return new Point(widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0);
    }
}
