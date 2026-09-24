package sophisticated.building.neoforge;

import net.minecraft.core.component.DataComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.transfer.item.ItemAccessItemHandler;
import sophisticated.building.ClientConfig;
import sophisticated.building.CommonConfig;
import sophisticated.building.ServerConfig;
import sophisticated.building.SophisticatedBuilding;
import sophisticated.building.compatibility.CompatHelper;
import sophisticated.building.item.AbstractRandomizerBagItem;
import sophisticated.building.neoforge.platform.NeoForgePlatformHelper;

@Mod(SophisticatedBuilding.MODID)
public class SophisticatedBuildingNeoForge {

    public SophisticatedBuildingNeoForge(IEventBus modEventBus, ModContainer container, Dist dist) {
        // Fills the deferred registers, which must happen before they are registered to the mod bus.
        SophisticatedBuilding.init();

        modEventBus.addListener(SophisticatedBuildingNeoForge::setup);
        modEventBus.addListener(SophisticatedBuildingNeoForge::registerCapabilities);
        modEventBus.addListener(NeoForgeNetworking::setupPackets);

        NeoForgePlatformHelper.registerDeferredRegisters(modEventBus);
        NeoForgeAttachments.register(modEventBus);

        // Register config
        container.registerConfig(ModConfig.Type.COMMON, (ModConfigSpec) CommonConfig.spec);
        container.registerConfig(ModConfig.Type.SERVER, (ModConfigSpec) ServerConfig.spec);
        if (dist.isClient()) {
            container.registerConfig(ModConfig.Type.CLIENT, (ModConfigSpec) ClientConfig.spec);
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
            SophisticatedBuildingNeoForgeClient.onConstructorClient(modEventBus);
        }
    }

    public static void setup(final FMLCommonSetupEvent event) {
        if (CompatHelper.isSophisticatedBackpacksLoaded()) {
            // Registers the upgrade containers through the integration, so SophisticatedCore is never loaded at class init time
            event.enqueueWork(SophisticatedBuilding::registerBackpacksUpgradeContainers);
        }
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Item capability of the randomizer bags (NeoForge 21.9+ transfer API), for other mods: their container component
        event.registerItem(Capabilities.Item.ITEM, (stack, access) -> {
            if (stack.getItem() instanceof AbstractRandomizerBagItem bagItem) {
                return new ItemAccessItemHandler(access, DataComponents.CONTAINER, bagItem.getInventorySize());
            }
            return null;
        }, SophisticatedBuilding.RANDOMIZER_BAG_ITEM.get(), SophisticatedBuilding.GOLDEN_RANDOMIZER_BAG_ITEM.get(),
                SophisticatedBuilding.DIAMOND_RANDOMIZER_BAG_ITEM.get(), SophisticatedBuilding.OMEGA_RANDOMIZER_BAG_ITEM.get());
    }
}
