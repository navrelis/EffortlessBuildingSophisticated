package sophisticated.building.config;

import java.util.List;

/**
 * Defines the options of one config file, mirroring the NeoForge {@code ModConfigSpec.Builder}
 * calls. Sections are one level deep (push/pop).
 */
public interface IConfigBuilder {

    /** Comment for the next section or option; several lines are joined as the loader does. */
    IConfigBuilder comment(String... lines);

    IConfigBuilder push(String section);

    IConfigBuilder pop();

    /** The next option only takes effect after a world restart (NeoForge); ignored on Fabric. */
    IConfigBuilder worldRestart();

    /** The next option is never sent from the server to clients (Fabric sync); ignored on NeoForge. */
    IConfigBuilder notSynced();

    ConfigValue<Boolean> define(String key, boolean defaultValue);

    ConfigValue<List<String>> defineList(String key, List<String> defaultValue);

    ConfigValue<Integer> defineInRange(String key, int defaultValue, int min, int max);

    ConfigValue<Double> defineInRange(String key, double defaultValue, double min, double max);

    /** Finishes the file and returns the loader's spec object (Fabric {@code ConfigSpec}, NeoForge {@code ModConfigSpec}). */
    Object build();
}
