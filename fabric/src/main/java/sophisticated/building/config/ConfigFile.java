package sophisticated.building.config;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads a {@link ConfigSpec} from its JSON file: a missing file is created with defaults, and an
 * unparseable file is left untouched while the defaults are used (the user must fix it). When
 * loading finds anything to correct (missing keys, clamped/wrong-typed values, unknown keys) the
 * original file is first backed up next to it (NeoForge-style {@code <name>.json.bak},
 * {@code <name>-1.json.bak}, ...), then the corrected file is written, so the same warning does
 * not repeat on every load.
 */
public final class ConfigFile {

    private static final String BACKUP_SUFFIX = ".bak";

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
            content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
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
        for (String unknownKey : result.unknownKeys()) {
            logger.warn("Config file {}: unknown option {} ignored", file.getFileName(), unknownKey);
        }
        if (result.needsCorrection()) {
            Path backup = backup(file, logger);
            if (backup != null) {
                logger.warn("Config file {} is not correct. Correcting, previous file backed up to {}",
                        file.getFileName(), backup.getFileName());
            }
            write(spec, file, logger);
        }
    }

    /**
     * Copies {@code file} to {@code <name>.json.bak}, or {@code <name>-1.json.bak}, {@code -2}, ...
     * if that already exists, mirroring NeoForge's {@code <name>-1.toml.bak} backups.
     */
    private static Path backup(Path file, Logger logger) {
        String fileName = file.getFileName().toString();
        String base = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - ".json".length()) : fileName;

        Path backup = file.resolveSibling(base + ".json" + BACKUP_SUFFIX);
        for (int i = 1; Files.exists(backup); i++) {
            backup = file.resolveSibling(base + "-" + i + ".json" + BACKUP_SUFFIX);
        }

        try {
            Files.copy(file, backup, StandardCopyOption.COPY_ATTRIBUTES);
            return backup;
        } catch (IOException e) {
            logger.error("Could not back up config file {}: {}", file, e.toString());
            return null;
        }
    }

    /** Writes the spec's current values to its file in {@code directory} (e.g. after the settings screen changed them). */
    public static void save(ConfigSpec spec, Path directory, Logger logger) {
        write(spec, directory.resolve(spec.getFileName()), logger);
    }

    private static void write(ConfigSpec spec, Path file, Logger logger) {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(temp, (spec.toFileJson() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Could not write config file {}: {}", file, e.toString());
        }
    }
}
