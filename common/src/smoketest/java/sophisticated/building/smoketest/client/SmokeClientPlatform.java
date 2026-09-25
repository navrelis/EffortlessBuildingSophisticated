package sophisticated.building.smoketest.client;

import net.minecraft.client.KeyMapping;

import sophisticated.building.smoketest.SmokeTest;

/**
 * Optional client glue of a loader, registered in {@code META-INF/services} by its smoke source set. Loaders without one
 * (Fabric) get the defaults.
 */
public interface SmokeClientPlatform {

    SmokeClientPlatform DEFAULT = new SmokeClientPlatform() {
    };

    static SmokeClientPlatform get() {
        return SmokeTest.firstService(SmokeClientPlatform.class).orElse(DEFAULT);
    }

    /**
     * Makes a held key mapping count as held while a screen is open, until the returned handle is closed. NeoForge and
     * Forge report an IN_GAME key as up while a screen is open, and the mod then falls back to the physical key state,
     * which a harness cannot press; there the glue switches the mapping's conflict context. Vanilla/Fabric: nothing to do.
     */
    default Runnable keepHeldInScreens(KeyMapping mapping) {
        return () -> {
        };
    }
}
