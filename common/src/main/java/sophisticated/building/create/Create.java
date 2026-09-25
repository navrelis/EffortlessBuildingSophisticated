package sophisticated.building.create;

import net.minecraft.resources.ResourceLocation;
import sophisticated.building.SophisticatedBuilding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Create {
    public static final String ID = SophisticatedBuilding.MODID;

    // Minecraft 1.16.5 logs through Log4j only (SLF4J and com.mojang.logging.LogUtils arrive with 1.18)
    public static final Logger LOGGER = LogManager.getLogger(Create.class);

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(SophisticatedBuilding.MODID, path);
    }
}
