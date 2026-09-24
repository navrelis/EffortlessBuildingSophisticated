package sophisticated.building.create;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import sophisticated.building.SophisticatedBuilding;
import org.slf4j.Logger;

public class Create {
    public static final String ID = SophisticatedBuilding.MODID;

    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(SophisticatedBuilding.MODID, path);
    }
}
