package sophisticated.building.create;

import net.minecraft.resources.ResourceLocation;
import sophisticated.building.SophisticatedBuilding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Create {
    public static final String ID = SophisticatedBuilding.MODID;

    // Minecraft 1.18.1 has no com.mojang.logging.LogUtils (1.18.2+)
    public static final Logger LOGGER = LoggerFactory.getLogger(Create.class);

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(SophisticatedBuilding.MODID, path);
    }
}
