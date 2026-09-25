package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.gui.SliderValues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SliderValuesTest {

    @Test
    void linearIntegerSliderSnapsToWholeSteps() {
        SliderValues ticks = SliderValues.linear(0, 100, 1);
        assertEquals(0, ticks.intValueAt(0));
        assertEquals(100, ticks.intValueAt(1));
        assertEquals(50, ticks.intValueAt(0.5));
        assertEquals(5, ticks.intValueAt(0.049));
        assertEquals(0.05, ticks.positionOf(5), 1e-9);
    }

    @Test
    void stepsCountFromTheMinimumAndTheEndsStayReachable() {
        SliderValues distance = SliderValues.linear(16, 256, 8);
        assertEquals(16, distance.intValueAt(0));
        assertEquals(256, distance.intValueAt(1));
        assertEquals(64, distance.snap(66));
        assertEquals(72, distance.snap(69));
        // 100000 is no multiple of 64, but the slider's right end still reaches it
        SliderValues previews = new SliderValues(0, 100000, 64, 3);
        assertEquals(100000, previews.intValueAt(1));
        assertEquals(99968, previews.snap(99990));
    }

    @Test
    void decimalStepsHaveNoFloatingPointNoise() {
        SliderValues scale = SliderValues.linear(0.05, 1.0, 0.05);
        assertEquals(0.25, scale.snap(0.26));
        assertEquals(0.3, scale.snap(0.31));
        assertEquals(0.05, scale.valueAt(0));
        assertEquals(1.0, scale.valueAt(1));
    }

    @Test
    void exponentGivesTheLowValuesMoreRoomAndRoundTrips() {
        SliderValues previews = new SliderValues(0, 100000, 64, 3);
        double defaultPosition = previews.positionOf(4096);
        assertTrue(defaultPosition > 0.3 && defaultPosition < 0.4, "4096 of 100000 sits at about a third: " + defaultPosition);
        assertEquals(4096, previews.intValueAt(defaultPosition));
        for (int value : new int[]{0, 64, 1024, 4096, 16384, 65536, 100000}) {
            assertEquals(value, previews.intValueAt(previews.positionOf(value)), "round trip of " + value);
        }
    }

    @Test
    void positionsAndValuesAreClamped() {
        SliderValues ticks = SliderValues.linear(0, 100, 1);
        assertEquals(0, ticks.intValueAt(-0.5));
        assertEquals(100, ticks.intValueAt(1.5));
        assertEquals(0, ticks.positionOf(-10));
        assertEquals(1, ticks.positionOf(1000));
    }

    @Test
    void invalidRangesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SliderValues.linear(5, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> SliderValues.linear(0, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new SliderValues(0, 10, 1, 0.5));
    }
}
