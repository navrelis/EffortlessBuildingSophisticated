package sophisticated.building.smoketest.backpack;

import net.minecraft.client.gui.screens.Screen;
import sophisticated.building.smoketest.SmokeTest;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Client side of the Sophisticated Backpacks fixture: finds the mod's widgets in a Sophisticated Backpacks storage
 * screen so the harness can click them where a player would. Implemented next to {@link SmokeBackpacks} in
 * {@code common/src/smoketestBackpacks} and registered in {@code META-INF/services}; a separate service because it
 * references client classes (only the client scenarios load it). Every method must run on the client thread.
 */
public interface SmokeBackpackScreens {

    /** The screens of this loader build, or empty when it has no Sophisticated Backpacks. */
    static Optional<SmokeBackpackScreens> find() {
        return SmokeTest.firstService(SmokeBackpackScreens.class);
    }

    /** A point on the screen in GUI coordinates. */
    static final class Point {
        private final double x;
        private final double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }
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
