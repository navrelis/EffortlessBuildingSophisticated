package sophisticated.building.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
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
 * Forge entry point for Minecraft 1.21 (Forge 51). Sophisticated Backpacks has no Forge build for this Minecraft
 * version, so this loader ships no backpack integration (the Building Upgrades stay placeholder items).
 * <p>
 * Forge 51 constructs mods only through a no-argument constructor (the {@code FMLJavaModLoadingContext} parameter of
 * the 1.21.1 build is Forge 52+), so the mod bus and the config registration come from the static contexts.
 */
@Mod(SophisticatedBuilding.MODID)
public class SophisticatedBuildingForge {

    public SophisticatedBuildingForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext context = ModLoadingContext.get();

        // Fills the deferred registers, which must happen before they are registered to the mod bus.
        SophisticatedBuilding.init();

        ForgePlatformHelper.registerDeferredRegisters(modEventBus);
        ForgeNetworking.setupPackets();

        // Register config (the SERVER config is per world; Forge syncs it to the clients)
        context.registerConfig(ModConfig.Type.COMMON, (ForgeConfigSpec) CommonConfig.spec);
        context.registerConfig(ModConfig.Type.SERVER, (ForgeConfigSpec) ServerConfig.spec);
        if (FMLEnvironment.dist.isClient()) {
            context.registerConfig(ModConfig.Type.CLIENT, (ForgeConfigSpec) ClientConfig.spec);
            SophisticatedBuildingForgeClient.onConstructorClient(modEventBus);
        }
    }
}
