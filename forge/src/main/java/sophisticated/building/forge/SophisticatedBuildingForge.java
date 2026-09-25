package sophisticated.building.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import sophisticated.building.ClientConfig;
import sophisticated.building.CommonConfig;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.forge.platform.ForgePlatformHelper;

/**
 * Forge entry point. The Sophisticated Backpacks integration is registered as a service and only active when
 * Sophisticated Backpacks is installed.
 */
@Mod(SophisticatedBuilding.MODID)
public class SophisticatedBuildingForge {

    public SophisticatedBuildingForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Fills the deferred registers, which must happen before they are registered to the mod bus.
        SophisticatedBuilding.init();

        modEventBus.addListener(SophisticatedBuildingForge::setup);
        modEventBus.addListener(PowerLevelCapability::register);

        ForgePlatformHelper.registerDeferredRegisters(modEventBus);
        ForgeNetworking.setupPackets();

        // Register config (the SERVER config is per world; Forge syncs it to the clients)
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.COMMON, (ForgeConfigSpec) CommonConfig.spec);
        context.registerConfig(ModConfig.Type.SERVER, (ForgeConfigSpec) ServerConfig.spec);
        if (FMLEnvironment.dist.isClient()) {
            context.registerConfig(ModConfig.Type.CLIENT, (ForgeConfigSpec) ClientConfig.spec);
            SophisticatedBuildingForgeClient.onConstructorClient(modEventBus);
        }
    }

    public static void setup(final FMLCommonSetupEvent event) {
        if (CompatHelper.isSophisticatedBackpacksLoaded()) {
            // Registers the upgrade containers through the integration, so SophisticatedCore is never loaded at class init time
            event.enqueueWork(SophisticatedBuilding::registerBackpacksUpgradeContainers);
        }
    }
}
