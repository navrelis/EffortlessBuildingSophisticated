package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.gui.TitleFit;

import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleFitTest {

    /** A monospaced stand-in for the game font: 6 pixels per character (the vanilla font's typical width). */
    private static final ToIntFunction<String> WIDTH = text -> text.length() * 6;
    /** A randomizer bag texture is 176 wide, the title starts at 8 and keeps 8 free at the right. */
    private static final int AVAILABLE = 160;

    @Test
    void shortTitleIsDrawnAtFullSize() {
        TitleFit fit = TitleFit.fit("Omega Randomizer Bag", AVAILABLE, WIDTH);
        assertEquals(1f, fit.scale());
        assertEquals("Omega Randomizer Bag", fit.text());
        assertFalse(fit.truncated());
    }

    @Test
    void slightlyTooWideTitleIsScaledToExactlyTheAvailableWidth() {
        String title = "Sophisticated Golden Randomizer Bag"; // 35 characters = 210 px
        TitleFit fit = TitleFit.fit(title, AVAILABLE, WIDTH);
        assertEquals(AVAILABLE / 210f, fit.scale(), 1e-6);
        assertTrue(fit.scale() >= TitleFit.MIN_SCALE);
        assertEquals(title, fit.text());
        assertFalse(fit.truncated());
        assertEquals(AVAILABLE, fit.drawnWidth(WIDTH), 1e-3);
    }

    @Test
    void veryLongTitleIsCutWithEllipsisAtTheMinimumScaleAndStillFits() {
        String title = "My very own long renamed randomizer bag full of stone and more";
        TitleFit fit = TitleFit.fit(title, AVAILABLE, WIDTH);
        assertEquals(TitleFit.MIN_SCALE, fit.scale());
        assertTrue(fit.truncated());
        assertTrue(fit.text().endsWith(TitleFit.ELLIPSIS));
        assertTrue(title.startsWith(fit.text().substring(0, fit.text().length() - TitleFit.ELLIPSIS.length())));
        assertTrue(fit.drawnWidth(WIDTH) <= AVAILABLE, "drawn width " + fit.drawnWidth(WIDTH));
        // As long as possible: one more character would not fit
        String longer = title.substring(0, fit.text().length() - TitleFit.ELLIPSIS.length() + 1) + TitleFit.ELLIPSIS;
        assertTrue(WIDTH.applyAsInt(longer) * TitleFit.MIN_SCALE > AVAILABLE);
    }

    @Test
    void noTrailingSpaceBeforeTheEllipsis() {
        // 160 / 0.6 = 266 px: 41 characters + "..." fit, and the 41st character is a space, which is dropped
        String title = "Randomizer bag with a rather long name x and more";
        TitleFit fit = TitleFit.fit(title, AVAILABLE, WIDTH);
        assertTrue(fit.truncated());
        assertFalse(fit.text().contains(" " + TitleFit.ELLIPSIS), fit.text());
        assertEquals("Randomizer bag with a rather long name x...", fit.text());
    }

    @Test
    void nothingFitsGivesOnlyTheEllipsis() {
        TitleFit fit = TitleFit.fit("Anything", 5, WIDTH);
        assertEquals(TitleFit.ELLIPSIS, fit.text());
        assertTrue(fit.truncated());
    }
}
