package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.buildmode.BuildModeHistory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildModeHistoryTest {

    private enum Mode { DISABLED, SINGLE, LINE, WALL }

    private static BuildModeHistory<Mode> history() {
        return new BuildModeHistory<>(Mode.DISABLED, Mode.DISABLED, Mode.SINGLE);
    }

    @Test
    void previousIsTheModeChosenBeforeTheCurrentOneWhereverItWasChosen() {
        BuildModeHistory<Mode> history = history();
        history.changeTo(Mode.LINE); // radial menu
        history.changeTo(Mode.WALL); // radial menu
        assertEquals(Mode.LINE, history.previous());
        history.changeTo(history.previous()); // "Activate Previous Build Mode"
        assertEquals(Mode.LINE, history.current());
        assertEquals(Mode.WALL, history.previous());
        history.changeTo(history.previous());
        assertEquals(Mode.WALL, history.current());
    }

    @Test
    void reselectingTheCurrentModeKeepsThePrevious() {
        BuildModeHistory<Mode> history = history();
        history.changeTo(Mode.LINE);
        history.changeTo(Mode.WALL);
        history.changeTo(Mode.WALL);
        assertEquals(Mode.LINE, history.previous());
    }

    @Test
    void disableToggleReturnsToTheModeActiveBeforeDisabling() {
        BuildModeHistory<Mode> history = history();
        assertEquals(Mode.SINGLE, history.disableToggleTarget(), "nothing used yet: the default");
        history.changeTo(Mode.LINE);
        history.changeTo(Mode.WALL);
        assertEquals(Mode.DISABLED, history.disableToggleTarget());
        history.changeTo(history.disableToggleTarget());
        assertEquals(Mode.WALL, history.disableToggleTarget());
        // Disabled chosen in the radial menu instead of the toggle key: same result
        history.changeTo(Mode.LINE);
        history.changeTo(Mode.DISABLED);
        assertEquals(Mode.LINE, history.disableToggleTarget());
        assertEquals(Mode.LINE, history.previous());
    }
}
