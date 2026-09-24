package sophisticated.building.platform;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.platform.services.IBackpackIntegration;
import sophisticated.building.platform.services.IBlockEventHelper;
import sophisticated.building.platform.services.IConfigHelper;
import sophisticated.building.platform.services.INetworkHelper;
import sophisticated.building.platform.services.IPlatformHelper;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Access point for everything that differs between mod loaders. Each loader project registers its
 * implementation of every service interface in {@code META-INF/services/<interface name>}, and the
 * loader-neutral code only talks to these interfaces. Client-only services live in
 * {@link ClientServices} so a dedicated server never loads them.
 */
public final class Services {

    // Own logger: SophisticatedBuilding registers its content through these services during its static initialisation.
    private static final Logger LOGGER = LogManager.getLogger("sophisticatedbuilding");

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IBlockEventHelper BLOCK_EVENTS = load(IBlockEventHelper.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);
    public static final IConfigHelper CONFIG = load(IConfigHelper.class);

    private static volatile IBackpackIntegration backpackIntegration;

    private Services() {
    }

    /**
     * The Sophisticated Backpacks integration, or {@link IBackpackIntegration#NONE} while Sophisticated
     * Backpacks is not loaded or when this loader build ships no integration (no service registered,
     * e.g. a Minecraft version without a Sophisticated Backpacks build). Like
     * {@link CompatHelper#isSophisticatedBackpacksLoaded()}, a miss is re-checked on later calls.
     */
    public static IBackpackIntegration backpacks() {
        IBackpackIntegration integration = backpackIntegration;
        if (integration != null) {
            return integration;
        }
        if (!CompatHelper.isSophisticatedBackpacksLoaded()) {
            return IBackpackIntegration.NONE;
        }
        try {
            integration = loadOptional(IBackpackIntegration.class).orElse(IBackpackIntegration.NONE);
        } catch (Exception | LinkageError e) {
            LOGGER.warn("Failed to load the Sophisticated Backpacks integration: {}", e.toString());
            integration = IBackpackIntegration.NONE;
        }
        backpackIntegration = integration;
        return integration;
    }

    public static <T> T load(Class<T> clazz) {
        return loadOptional(clazz)
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }

    public static <T> Optional<T> loadOptional(Class<T> clazz) {
        Optional<T> loadedService = ServiceLoader.load(clazz, Services.class.getClassLoader()).findFirst();
        loadedService.ifPresent(service -> LOGGER.debug("Loaded {} for service {}", service, clazz));
        return loadedService;
    }
}
