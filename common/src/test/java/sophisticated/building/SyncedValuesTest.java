package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.SyncedValues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncedValuesTest {

	@Test
	void clientUsesTheServerValuesWhileSynced() {
		SyncedValues values = new SyncedValues();
		assertEquals(8, values.pick(true, 1, 8));
		values.apply(new int[]{200, 4});
		assertTrue(values.isSynced());
		assertEquals(4, values.pick(true, 1, 8));
		assertEquals(8, values.pick(false, 1, 8));   // the server keeps its own config
		assertEquals(7, values.pick(true, 5, 7));    // unknown index: local value
		values.clear();
		assertFalse(values.isSynced());
		assertEquals(8, values.pick(true, 1, 8));
	}
}
