package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.gui.buildmode.RadialButtonLayout;
import sophisticated.building.gui.buildmode.RadialButtonLayout.Cell;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadialButtonLayoutTest {

    // RadialMenu's values: buttonDistance 105, ring outer edge 65 (+20 clearance), option rows 39 apart from y -13 with a
    // 14 px label above and 24 px kept free at the bottom right; action rows 26 apart from y -39
    private static final double PREFERRED = 105;
    private static final double MIN_INNER = 85;

    /** Option button counts per row of every build mode (BuildModeEnum options: speed 2, thickness 3, fill 2, ...). */
    private static final List<int[]> MODE_OPTION_COUNTS = List.of(
            new int[]{2}, new int[]{3}, new int[]{2}, new int[]{2}, new int[]{3}, new int[]{3}, new int[]{2}, new int[]{2},
            new int[]{2, 2}, new int[]{2, 2}, new int[]{2, 2}, new int[]{2}, new int[]{2, 2}, new int[]{2, 2, 2, 5});

    /** GUI sizes: the smallest Minecraft allows (320x240), the smoke window, 1920x1080 at GUI scale 4, 3 and 2, 1280x720 at 2. */
    private static final double[][] SIZES = {{320, 240}, {427, 240}, {480, 270}, {640, 360}, {960, 540}, {640, 360}, {341, 256}};

    private static List<int[]> rows(int... counts) {
        List<int[]> rows = new ArrayList<>();
        for (int count : counts) {
            int[] columns = new int[count];
            for (int i = 0; i < count; i++) columns[i] = i;
            rows.add(columns);
        }
        return rows;
    }

    private static List<int[]> leftRows(boolean canReplace) {
        List<int[]> rows = new ArrayList<>();
        rows.add(new int[]{2});
        rows.addAll(canReplace ? rows(5, 4) : rows(4));
        return rows;
    }

    private static RadialButtonLayout.Result right(int[] counts, double w, double h) {
        return RadialButtonLayout.layout(true, rows(counts), w, h, PREFERRED, MIN_INNER, -13, 39, 14, 24);
    }

    private static RadialButtonLayout.Result left(boolean canReplace, double w, double h) {
        return RadialButtonLayout.layout(false, leftRows(canReplace), w, h, PREFERRED, MIN_INNER, -39, 26, 0, 0);
    }

    private static void assertFits(List<Cell> cells, double w, double h, double bottomReserve, String what) {
        for (Cell c : cells) {
            assertTrue(c.left() >= -w / 2 && c.right() <= w / 2 && c.top() >= -h / 2 && c.bottom() <= h / 2 - bottomReserve,
                    what + " " + w + "x" + h + ": " + c + " outside the screen");
            assertTrue(Math.min(Math.abs(c.left()), Math.abs(c.right())) >= MIN_INNER - RadialButtonLayout.HALF,
                    what + " " + w + "x" + h + ": " + c + " reaches into the ring");
            for (Cell o : cells) {
                boolean overlap = o != c && c.left() < o.right() && o.left() < c.right() && c.top() < o.bottom() && o.top() < c.bottom();
                assertTrue(!overlap, what + " " + w + "x" + h + ": " + c + " overlaps " + o);
            }
        }
    }

    @Test
    void everyModesOptionsAndBothActionSetsFitEveryScreenSize() {
        for (double[] size : SIZES) {
            for (int[] counts : MODE_OPTION_COUNTS) {
                assertFits(right(counts, size[0], size[1]).cells(), size[0], size[1], 24, "options " + counts.length + " rows");
            }
            assertFits(left(true, size[0], size[1]).cells(), size[0], size[1], 0, "actions (replace)");
            assertFits(left(false, size[0], size[1]).cells(), size[0], size[1], 0, "actions");
        }
    }

    @Test
    void wideScreensKeepTheOriginalPositions() {
        List<Cell> options = right(new int[]{2, 2, 2, 5}, 960, 540).cells();
        for (Cell c : options) {
            assertEquals(PREFERRED + c.column() * RadialButtonLayout.PITCH, c.x());
            assertEquals(-13 + c.row() * 39, c.y());
        }
        List<Cell> actions = left(true, 960, 540).cells();
        assertEquals(-(PREFERRED + 52), actions.get(0).x()); // player settings above modifier settings
        assertEquals(-39, actions.get(0).y());
        for (Cell c : actions.subList(1, actions.size())) {
            assertEquals(-(PREFERRED + c.column() * RadialButtonLayout.PITCH), c.x());
            assertEquals(c.row() == 1 ? -13 : 13, c.y());
        }
    }

    @Test
    void theSmokeWindowShiftsTheTerrainRowInsteadOfCuttingIt() {
        // 854x480 at GUI scale 2: the 5 terrain shapes ended 5 px past the right edge at the preferred position
        RadialButtonLayout.Result result = right(new int[]{2, 2, 2, 5}, 427, 240);
        assertTrue(result.inner() < PREFERRED && result.inner() >= MIN_INNER, "inner " + result.inner());
        double rightmost = result.cells().stream().mapToDouble(Cell::right).max().orElseThrow();
        assertTrue(rightmost <= 427 / 2.0 - RadialButtonLayout.MARGIN, "rightmost " + rightmost);
    }

    @Test
    void theSmallestScreenWrapsLongRowsIntoExtraLines() {
        RadialButtonLayout.Result result = right(new int[]{2, 2, 2, 5}, 320, 240);
        long terrainLines = result.cells().stream().filter(c -> c.row() == 3).mapToDouble(Cell::y).distinct().count();
        assertEquals(2, terrainLines);
        long actionLines = left(true, 320, 240).cells().stream().filter(c -> c.row() == 1).mapToDouble(Cell::y).distinct().count();
        assertEquals(2, actionLines);
    }
}
