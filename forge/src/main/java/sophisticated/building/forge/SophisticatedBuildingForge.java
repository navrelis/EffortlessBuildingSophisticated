package sophisticated.building.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import sophisticated.building.ClientConfig;
import sophisticated.building.CommonConfig;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.forge.platform.ForgePlatformHelper;

/**
 * Forge entry point. Sophisticated Backpacks has no Forge build for this Minecraft version, so this
 * loader ships no backpack integration (the Building Upgrades stay placeholder items).
 */
@Mod(SophisticatedBuilding.MODID)
public class SophisticatedBuildingForge {

    public SophisticatedBuildingForge(FMLJavaModLoadingContext context) {
        BusGroup modBusGroup = context.getModBusGroup();

        // Fills the deferred registers, which must happen before they are registered to the mod bus.
        SophisticatedBuilding.init();

        ForgePlatformHelper.registerDeferredRegisters(modBusGroup);
        ForgeNetworking.setupPackets();

        // Register config (the SERVER config is per world; Forge syncs it to the clients)
        context.registerConfig(ModConfig.Type.COMMON, (ForgeConfigSpec) CommonConfig.spec);
        context.registerConfig(ModConfig.Type.SERVER, (ForgeConfigSpec) ServerConfig.spec);
        if (FMLEnvironment.dist.isClient()) {
            context.registerConfig(ModConfig.Type.CLIENT, (ForgeConfigSpec) ClientConfig.spec);
            SophisticatedBuildingForgeClient.onConstructorClient(modBusGroup);
        }
    }
}
