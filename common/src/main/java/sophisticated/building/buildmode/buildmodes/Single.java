package sophisticated.building.buildmode.buildmodes;

import sophisticated.building.buildmode.IBuildMode;
import sophisticated.building.utilities.BlockSet;

public class Single implements IBuildMode {

	@Override
	public void initialize() {

	}

	@Override
	public boolean onClick(BlockSet blocks) {
		return true;
	}

	@Override
	public void findCoordinates(BlockSet blocks) {
		//Do nothing
	}
}
