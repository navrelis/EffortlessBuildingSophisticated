package sophisticated.building.platform;

import sophisticated.building.platform.services.IClientHelper;

/**
 * Client-only platform services, kept apart from {@link Services} so a dedicated server never loads
 * their implementations.
 */
public final class ClientServices {

    public static final IClientHelper CLIENT = Services.load(IClientHelper.class);

    private ClientServices() {
    }
}
