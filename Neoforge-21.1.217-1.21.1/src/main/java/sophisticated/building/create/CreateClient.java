package sophisticated.building.create;

import net.createmod.catnip.render.SuperByteBufferCache;
import sophisticated.building.create.foundation.utility.ghost.GhostBlocks;

public class CreateClient {
    public static final SuperByteBufferCache BUFFER_CACHE = new SuperByteBufferCache();
    public static final GhostBlocks GHOST_BLOCKS = new GhostBlocks();

    public static void invalidateRenderers() {
        CreateClient.BUFFER_CACHE.invalidate();
    }
}
