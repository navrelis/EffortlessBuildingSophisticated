package sophisticated.building.smoketest.fabric;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import sophisticated.building.smoketest.server.SmokeServer;
import sophisticated.building.smoketest.server.SmokeServerTests;

/**
 * Fabric glue of the smoke harness (both sides): registers the server scenarios as game test functions (their test
 * instances are data, see {@link SmokeServerTests}) and starts the server smoke run when runSmokeServer launched the game.
 */
public final class FabricSmokeTest implements ModInitializer {
    @Override
    public void onInitialize() {
        SmokeServerTests.functions().forEach((name, function) ->
                Registry.register(BuiltInRegistries.TEST_FUNCTION, SmokeServerTests.functionKey(name), function));
        SmokeServer.init();
    }
}
