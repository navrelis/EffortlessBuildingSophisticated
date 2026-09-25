package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.render.PreviewRules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewRulesTest {

	@Test
	void nothingToShowWithoutBlocks() {
		assertFalse(PreviewRules.showsLookAtPreview(0, false, true, false, false));
		assertFalse(PreviewRules.showsLookAtPreview(0, true, true, true, false));
	}

	@Test
	void disableModeSingleBlockIsVanillasWithoutQuickReplace() {
		assertFalse(PreviewRules.showsLookAtPreview(1, true, false, true, true));
		assertFalse(PreviewRules.showsLookAtPreview(1, true, false, true, false));
	}

	@Test
	void disableModeWithQuickReplaceShowsTheSingleReplacedBlock() {
		// whatever the "only while building" config: a Disable mode click is always idle
		assertTrue(PreviewRules.showsLookAtPreview(1, true, true, true, true));
		assertTrue(PreviewRules.showsLookAtPreview(1, true, true, true, false));
	}

	@Test
	void otherModesKeepTheOnlyWhileBuildingConfigForASingleBlock() {
		assertFalse(PreviewRules.showsLookAtPreview(1, false, false, true, true));
		assertFalse(PreviewRules.showsLookAtPreview(1, false, true, true, true));
		assertTrue(PreviewRules.showsLookAtPreview(1, false, false, true, false));
		assertTrue(PreviewRules.showsLookAtPreview(1, false, false, false, true));
	}

	@Test
	void severalBlocksAreAlwaysShown() {
		assertTrue(PreviewRules.showsLookAtPreview(2, true, false, true, true));
		assertTrue(PreviewRules.showsLookAtPreview(5, false, false, true, true));
	}
}
