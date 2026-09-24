package sophisticated.building.config;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads a {@link ConfigSpec} from its JSON file: a missing file is created with defaults, missing
 * keys are added (keeping the other values), and an unparseable file is left untouched while the
 * defaults are used.
 */
public final class ConfigFile {

    private ConfigFile() {
    }

    public static void load(ConfigSpec spec, Path directory, Logger logger) {
        Path file = directory.resolve(spec.getFileName());
        if (!Files.exists(file)) {
            spec.resetToDefaults();
            write(spec, file, logger);
            return;
        }

        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            spec.resetToDefaults();
            logger.error("Could not read config file {}, using defaults: {}", file, e.toString());
            return;
        }

        ConfigSpec.LoadResult result = spec.load(content);
        if (!result.parsed()) {
            logger.error("Config file {} is not valid JSON, using defaults and leaving the file unchanged: {}", file, result.error());
            return;
        }
        for (String warning : result.warnings()) {
            logger.warn("Config file {}: {}", file.getFileName(), warning);
        }
        if (result.missingKeys()) {
            logger.info("Config file {} is missing options, adding them with default values", file.getFileName());
            write(spec, file, logger);
        }
    }

    private static void write(ConfigSpec spec, Path file, Logger logger) {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, spec.toFileJson() + System.lineSeparator(), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Could not write config file {}: {}", file, e.toString());
        }
    }
}
