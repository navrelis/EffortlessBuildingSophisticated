package sophisticated.building;

import sophisticated.building.config.SimpleConfigValue;

import java.util.List;

public class ServerConfig {
    public static final Validation validation = new Validation();
    public static final Memory memory = new Memory();

    // Kept as a compatibility placeholder while config registration is migrated to Fabric.
    public static final Object spec = new Object();

    public static class Validation {
        public final SimpleConfigValue<Boolean> allowInSurvival = new SimpleConfigValue<>(true);
        public final SimpleConfigValue<Boolean> useWhitelist = new SimpleConfigValue<>(false);
        public final SimpleConfigValue<List<String>> whitelist = new SimpleConfigValue<>(List.of("Player1", "Player2"));
        public final SimpleConfigValue<Integer> maxBlocksPlacedAtOnce = new SimpleConfigValue<>(10000);
    }

    public static class Memory {
        public final SimpleConfigValue<Integer> undoStackSize = new SimpleConfigValue<>(50);
    }
}
