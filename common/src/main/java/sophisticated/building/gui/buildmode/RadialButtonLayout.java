package sophisticated.building.gui.buildmode;

import java.util.ArrayList;
import java.util.List;

/**
 * Places the radial menu's side buttons so they always stay inside the screen. One side of the ring holds rows of
 * buttons; a row's buttons have columns counted from the ring outwards (column 0 is next to the ring). At the
 * preferred layout the column nearest the ring is {@code preferredInner} from the screen centre, columns are
 * {@link #PITCH} apart and rows {@code rowPitch} apart starting at {@code firstRowY}. When the screen is too narrow the
 * block moves towards the ring (down to {@code minInner}), and when that is not enough a row wraps its outer columns
 * into extra lines below it. When the block is too tall it moves up. Pure math (GUI coordinates relative to the screen
 * centre, +x right, +y down), no Minecraft classes.
 */
public final class RadialButtonLayout {

    /** Distance between the centres of two neighbouring buttons. */
    public static final double PITCH = 26;
    /** Half the size of a button (buttons are 20 x 20). */
    public static final double HALF = 10;
    /** Space kept free between a button and the screen edge. */
    public static final double MARGIN = 4;

    /**
     * @param row    index of the row
     * @param column the button's column in its row (as given, before wrapping)
     * @param x      centre, relative to the screen centre
     * @param y      centre, relative to the screen centre
     */
    public record Cell(int row, int column, double x, double y) {
        public double left() {
            return x - HALF;
        }

        public double right() {
            return x + HALF;
        }

        public double top() {
            return y - HALF;
        }

        public double bottom() {
            return y + HALF;
        }
    }

    /**
     * The laid-out side.
     *
     * @param cells     one cell per button, in the order of {@code rows}
     * @param inner     distance of column 0 from the screen centre (the side's label x is derived from it)
     * @param rowFirstY centre y of the first line of every row
     */
    public record Result(List<Cell> cells, double inner, double[] rowFirstY) {
    }

    /**
     * @param right          true for the right side (columns grow to +x), false for the left side
     * @param rows           per row, the columns of its buttons (e.g. {0, 1, 2} or {2} for a single button above column 2)
     * @param screenWidth    GUI width
     * @param screenHeight   GUI height
     * @param preferredInner distance of column 0 from the centre when there is room
     * @param minInner       closest column 0 may come to the centre (keeps the ring and its labels clear)
     * @param firstRowY      centre y of the first row at the preferred layout
     * @param rowPitch       distance between rows (a row's label, if any, sits in this space)
     * @param headroom       space a row needs above its first line (e.g. for its label), counted from the button top
     * @param bottomReserve  space kept free at the bottom of the screen on this side (e.g. for text there)
     */
    public static Result layout(boolean right, List<int[]> rows, double screenWidth, double screenHeight, double preferredInner,
                                double minInner, double firstRowY, double rowPitch, double headroom, double bottomReserve) {
        // Horizontal: how many columns fit between minInner and the screen edge
        double outerLimit = screenWidth / 2 - MARGIN - HALF;
        int maxColumns = Math.max(1, (int) Math.floor((outerLimit - minInner) / PITCH) + 1);
        int widest = 0;
        for (int[] columns : rows) {
            for (int column : columns) widest = Math.max(widest, Math.min(column, maxColumns - 1) + 1);
        }
        double inner = Math.max(minInner, Math.min(preferredInner, outerLimit - (widest - 1) * PITCH));

        // Vertical: rows keep their pitch; wrapped lines add PITCH each
        double[] rowFirstY = new double[rows.size()];
        List<Cell> cells = new ArrayList<>();
        double y = firstRowY;
        for (int r = 0; r < rows.size(); r++) {
            int[] columns = rows.get(r);
            rowFirstY[r] = y;
            int lines = 1;
            for (int column : columns) {
                int line = column / maxColumns;
                lines = Math.max(lines, line + 1);
                double x = inner + (column % maxColumns) * PITCH;
                cells.add(new Cell(r, column, right ? x : -x, y + line * PITCH));
            }
            y += rowPitch + (lines - 1) * PITCH;
        }

        // Move the block up if it runs into the bottom (or the reserved space there), but not above the top
        double bottom = cells.stream().mapToDouble(Cell::bottom).max().orElse(0);
        double top = firstRowY - HALF - headroom;
        double maxBottom = screenHeight / 2 - MARGIN - bottomReserve;
        double shift = Math.min(Math.max(0, bottom - maxBottom), Math.max(0, top - (-screenHeight / 2 + MARGIN)));
        if (shift > 0) {
            List<Cell> shifted = new ArrayList<>(cells.size());
            for (Cell cell : cells) shifted.add(new Cell(cell.row(), cell.column(), cell.x(), cell.y() - shift));
            cells = shifted;
            for (int r = 0; r < rowFirstY.length; r++) rowFirstY[r] -= shift;
        }
        return new Result(List.copyOf(cells), inner, rowFirstY);
    }
}
