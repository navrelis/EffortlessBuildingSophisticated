package sophisticated.building.smoketest.backpack;

import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Client side of the Sophisticated Backpacks fixture: finds the mod's widgets in a Sophisticated Backpacks storage
 * screen so the harness can click them where a player would. Implemented next to {@link SmokeBackpacks} in
 * {@code common/src/smoketestBackpacks} and registered in {@code META-INF/services}; a separate service because it
 * references client classes (only the client scenarios load it). Every method must run on the client thread.
 */
public interface SmokeBackpackScreens {

    /** The screens of this loader build, or empty when it has no Sophisticated Backpacks. */
    static Optional<SmokeBackpackScreens> find() {
        return ServiceLoader.load(SmokeBackpackScreens.class, SmokeBackpackScreens.class.getClassLoader()).findFirst();
    }

    /** A point on the screen in GUI coordinates. */
    record Point(double x, double y) {
    }

    /** The screen is a Sophisticated Backpacks storage screen (the backpack screen). */
    boolean isStorageScreen(@Nullable Screen screen);

    /** Centre of the icon of the Building Upgrade's settings tab, or null when the screen shows no such tab. */
    @Nullable
    Point buildingTabIcon(Screen screen);

    /** The Building Upgrade's settings tab is the open tab. */
    boolean isBuildingTabOpen(Screen screen);

    /** Centre of the enable/disable toggle inside the open Building Upgrade settings tab, or null. */
    @Nullable
    Point buildingTabToggle(Screen screen);
}
