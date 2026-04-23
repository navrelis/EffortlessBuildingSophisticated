package sophisticated.building.create;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import sophisticated.building.SophisticatedBuilding;
import org.slf4j.Logger;

public class Create {
    public static final String ID = SophisticatedBuilding.MODID;

    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(SophisticatedBuilding.MODID, path);
    }
}
