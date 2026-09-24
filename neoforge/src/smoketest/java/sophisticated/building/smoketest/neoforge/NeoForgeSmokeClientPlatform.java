package sophisticated.building.smoketest.neoforge;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import sophisticated.building.smoketest.client.SmokeClientPlatform;

/** NeoForge client glue: an IN_GAME key mapping reads as up while a screen is open, so the harness widens its context. */
public final class NeoForgeSmokeClientPlatform implements SmokeClientPlatform {
    @Override
    public Runnable keepHeldInScreens(KeyMapping mapping) {
        IKeyConflictContext previous = mapping.getKeyConflictContext();
        mapping.setKeyConflictContext(KeyConflictContext.UNIVERSAL);
        return () -> mapping.setKeyConflictContext(previous);
    }
}
