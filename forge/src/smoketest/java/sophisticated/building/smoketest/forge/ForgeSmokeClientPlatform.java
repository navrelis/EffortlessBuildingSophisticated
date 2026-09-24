package sophisticated.building.smoketest.forge;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import sophisticated.building.smoketest.client.SmokeClientPlatform;

/** Forge client glue: an IN_GAME key mapping reads as up while a screen is open, so the harness widens its context. */
public final class ForgeSmokeClientPlatform implements SmokeClientPlatform {
    @Override
    public Runnable keepHeldInScreens(KeyMapping mapping) {
        IKeyConflictContext previous = mapping.getKeyConflictContext();
        mapping.setKeyConflictContext(KeyConflictContext.UNIVERSAL);
        return () -> mapping.setKeyConflictContext(previous);
    }
}
