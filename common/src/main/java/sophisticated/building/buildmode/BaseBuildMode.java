package sophisticated.building.buildmode;

import sophisticated.building.utilities.BlockSet;

public abstract class BaseBuildMode implements IBuildMode {

	protected int clicks;

	@Override
	public void initialize() {
		clicks = 0;
	}

	@Override
	public boolean onClick(BlockSet blocks) {
		clicks++;
		return false;
	}
}
