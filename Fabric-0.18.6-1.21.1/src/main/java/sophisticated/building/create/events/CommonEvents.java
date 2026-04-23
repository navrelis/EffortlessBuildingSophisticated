package sophisticated.building.create.events;

import net.createmod.catnip.data.WorldAttached;
import net.minecraft.world.level.LevelAccessor;

public class CommonEvents {

	public static void onUnloadWorld(LevelAccessor world) {
		WorldAttached.invalidateWorld(world);
	}


//	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class ModBusEvents {

//		@SubscribeEvent
//		public static void addPackFinders(AddPackFindersEvent event) {
//			if (event.getPackType() == PackType.CLIENT_RESOURCES) {
//				IModFileInfo modFileInfo = ModList.get().getModFileById(Create.ID);
//				if (modFileInfo == null) {
//					Create.LOGGER.error("Could not find Create mod file info; built-in resource packs will be missing!");
//					return;
//				}
//				IModFile modFile = modFileInfo.getFile();
//				event.addRepositorySource((consumer, constructor) -> {
//					consumer.accept(Pack.create(Create.asResource("legacy_copper").toString(), false, () -> new ModFilePackResources("Create Legacy Copper", modFile, "resourcepacks/legacy_copper"), constructor, Pack.Position.TOP, PackSource.DEFAULT));
//				});
//			}
//		}

	}

}
