package sophisticated.building.render;

/**
 * Pure decision whether the look-at preview (ghost blocks and outline) is drawn. No Minecraft imports so it is directly
 * unit-testable (see {@code PreviewRulesTest}).
 */
public final class PreviewRules {

    private PreviewRules() {
    }

    /**
     * @param blockCount       blocks the current build would place or break
     * @param buildModeDisabled the build mode is Disable
     * @param quickReplacing   Quick Replace is on
     * @param idle             no build is in progress (before the first click of a multi-click mode)
     * @param onlyWhenBuilding the client config hiding single-block previews while idle
     */
    public static boolean showsLookAtPreview(int blockCount, boolean buildModeDisabled, boolean quickReplacing, boolean idle,
                                             boolean onlyWhenBuilding) {
        if (blockCount <= 0) return false;
        if (blockCount > 1) return true;
        //Disable mode: a plain click is vanilla's (its own outline). With Quick Replace the mod replaces the block looked
        //at instead, a one-click build, so it always shows which block that is.
        if (buildModeDisabled) return quickReplacing;
        return !(idle && onlyWhenBuilding);
    }
}
