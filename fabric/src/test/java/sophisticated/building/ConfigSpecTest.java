package sophisticated.building;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sophisticated.building.config.ConfigFile;
import sophisticated.building.config.ConfigSpec;
import sophisticated.building.config.ConfigValue;
import sophisticated.building.config.ModConfigs;
import sophisticated.building.config.NumberConfigValue;
import sophisticated.building.config.SimpleConfigValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSpecTest {

    private static final Logger LOGGER = LogManager.getLogger(ConfigSpecTest.class);

    // The mod's config classes are loader-neutral; on Fabric they are backed by ConfigSpec/SimpleConfigValue
    private static ConfigSpec serverSpec() {
        return ModConfigs.spec(ServerConfig.spec);
    }

    private static <T> SimpleConfigValue<T> simple(ConfigValue<T> value) {
        return (SimpleConfigValue<T>) value;
    }

    private static final class TestConfig {
        final SimpleConfigValue<Boolean> enabled;
        final SimpleConfigValue<Integer> count;
        final SimpleConfigValue<Double> scale;
        final SimpleConfigValue<List<String>> names;
        final SimpleConfigValue<Integer> other;
        final ConfigSpec spec;

        TestConfig() {
            ConfigSpec.Builder builder = new ConfigSpec.Builder("test");
            builder.push("General");
            enabled = builder.comment("Enabled.").define("enabled", true);
            count = builder.comment("Count.").defineInRange("count", 10, 1, 100);
            scale = builder.defineInRange("scale", 0.25, 0.05, 1.0);
            names = builder.defineList("names", Arrays.asList("a", "b"));
            builder.pop();
            builder.comment("Second section.").push("Other");
            other = builder.defineInRange("other", 5, 0, 10);
            builder.pop();
            spec = builder.build();
        }
    }

    @Test
    void defaultFileRoundTripsWithoutChanges() {
        TestConfig config = new TestConfig();
        String json = config.spec.toFileJson();

        ConfigSpec.LoadResult result = config.spec.load(json);

        assertTrue(result.parsed());
        assertFalse(result.missingKeys());
        assertTrue(result.warnings().isEmpty());
        assertEquals(10, config.count.get());
        assertEquals(Arrays.asList("a", "b"), config.names.get());
        JsonObject general = new JsonParser().parse(json).getAsJsonObject().getAsJsonObject("General");
        assertEquals("Count. Range: 1 ~ 100. Default: 10", general.get("_comment_count").getAsString());
        assertEquals("Second section.", new JsonParser().parse(json).getAsJsonObject()
                .getAsJsonObject("Other").get("_comment").getAsString());
    }

    @Test
    void missingKeysGetDefaultsAndKeepUserValues() {
        TestConfig config = new TestConfig();

        ConfigSpec.LoadResult result = config.spec.load("{\"General\": {\"count\": 42, \"unknownKey\": 1}}");

        assertTrue(result.parsed());
        assertTrue(result.missingKeys());
        assertTrue(result.warnings().isEmpty());
        assertEquals(42, config.count.get());
        assertEquals(true, config.enabled.get());
        assertEquals(0.25, config.scale.get());
        assertEquals(5, config.other.get());
    }

    @Test
    void outOfRangeNumbersAreClamped() {
        TestConfig config = new TestConfig();

        ConfigSpec.LoadResult result = config.spec.load(
                "{\"General\": {\"enabled\": false, \"count\": 500, \"scale\": 0.0, \"names\": [\"x\"]}, \"Other\": {\"other\": -3}}");

        assertTrue(result.parsed());
        assertFalse(result.missingKeys());
        assertEquals(3, result.warnings().size());
        assertEquals(false, config.enabled.get());
        assertEquals(100, config.count.get());
        assertEquals(0.05, config.scale.get());
        assertEquals(0, config.other.get());
        assertEquals(Arrays.asList("x"), config.names.get());
    }

    @Test
    void wrongTypesFallBackToDefaults() {
        TestConfig config = new TestConfig();

        ConfigSpec.LoadResult result = config.spec.load(
                "{\"General\": {\"enabled\": \"yes\", \"count\": 2.5, \"scale\": 0.5, \"names\": \"x\"}, \"Other\": {\"other\": 7}}");

        assertTrue(result.parsed());
        assertEquals(3, result.warnings().size());
        assertEquals(true, config.enabled.get());
        assertEquals(10, config.count.get());
        assertEquals(0.5, config.scale.get());
        assertEquals(Arrays.asList("a", "b"), config.names.get());
        assertEquals(7, config.other.get());
    }

    @Test
    void invalidJsonKeepsDefaults() {
        TestConfig config = new TestConfig();
        config.count.set(77);

        ConfigSpec.LoadResult result = config.spec.load("{\"General\": {\"count\": 42,");

        assertFalse(result.parsed());
        assertEquals(10, config.count.get());
        assertFalse(config.spec.load("[1, 2]").parsed());
    }

    @Test
    void fileIsCreatedFilledAndNeverOverwrittenWhenInvalid(@TempDir Path dir) throws IOException {
        TestConfig config = new TestConfig();
        Path file = dir.resolve("sophisticatedbuilding-test.json");

        ConfigFile.load(config.spec, dir, LOGGER);
        assertTrue(Files.exists(file));
        assertEquals(config.spec.toFileJson().trim(), new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim());

        Files.write(file, ("{\"General\": {\"count\": 42}}").getBytes(StandardCharsets.UTF_8));
        ConfigFile.load(config.spec, dir, LOGGER);
        assertEquals(42, config.count.get());
        JsonObject rewritten = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(42, rewritten.getAsJsonObject("General").get("count").getAsInt());
        assertEquals(5, rewritten.getAsJsonObject("Other").get("other").getAsInt());

        String broken = "{\"General\": {\"count\": 50";
        Files.write(file, (broken).getBytes(StandardCharsets.UTF_8));
        ConfigFile.load(config.spec, dir, LOGGER);
        assertEquals(10, config.count.get());
        assertEquals(broken, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void outOfRangeValueIsCorrectedWithBackupAndSecondLoadIsClean(@TempDir Path dir) throws IOException {
        TestConfig config = new TestConfig();
        Path file = dir.resolve("sophisticatedbuilding-test.json");
        String original = "{\"General\": {\"enabled\": true, \"count\": 500, \"scale\": 0.25, \"names\": [\"a\", \"b\"]}, "
                + "\"Other\": {\"other\": 5}}";
        Files.write(file, (original).getBytes(StandardCharsets.UTF_8));

        ConfigFile.load(config.spec, dir, LOGGER);

        assertEquals(100, config.count.get());
        JsonObject rewritten = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(100, rewritten.getAsJsonObject("General").get("count").getAsInt());

        Path backup = dir.resolve("sophisticatedbuilding-test.json.bak");
        assertTrue(Files.exists(backup));
        assertEquals(original, new String(Files.readAllBytes(backup), StandardCharsets.UTF_8));

        // The corrected file itself has nothing left to fix.
        ConfigSpec.LoadResult secondResult = config.spec.load(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        assertTrue(secondResult.warnings().isEmpty());
        assertFalse(secondResult.missingKeys());
        assertTrue(secondResult.unknownKeys().isEmpty());

        // A second ConfigFile.load() on the already-corrected file must not create another backup.
        ConfigFile.load(config.spec, dir, LOGGER);
        assertFalse(Files.exists(dir.resolve("sophisticatedbuilding-test-1.json.bak")));
    }

    @Test
    void wrongTypeIsCorrectedWithBackupAndSecondLoadIsClean(@TempDir Path dir) throws IOException {
        TestConfig config = new TestConfig();
        Path file = dir.resolve("sophisticatedbuilding-test.json");
        String original = "{\"General\": {\"enabled\": \"yes\", \"count\": 10, \"scale\": 0.25, \"names\": [\"a\", \"b\"]}, "
                + "\"Other\": {\"other\": 5}}";
        Files.write(file, (original).getBytes(StandardCharsets.UTF_8));

        ConfigFile.load(config.spec, dir, LOGGER);

        assertEquals(true, config.enabled.get());
        JsonObject rewritten = new JsonParser().parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(true, rewritten.getAsJsonObject("General").get("enabled").getAsBoolean());

        Path backup = dir.resolve("sophisticatedbuilding-test.json.bak");
        assertTrue(Files.exists(backup));
        assertEquals(original, new String(Files.readAllBytes(backup), StandardCharsets.UTF_8));

        ConfigSpec.LoadResult secondResult = config.spec.load(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        assertTrue(secondResult.warnings().isEmpty());
        assertFalse(secondResult.missingKeys());
        assertTrue(secondResult.unknownKeys().isEmpty());

        ConfigFile.load(config.spec, dir, LOGGER);
        assertFalse(Files.exists(dir.resolve("sophisticatedbuilding-test-1.json.bak")));
    }

    @Test
    void unknownKeyIsDroppedFromFileButKeptInBackupAndReported(@TempDir Path dir) throws IOException {
        TestConfig config = new TestConfig();
        Path file = dir.resolve("sophisticatedbuilding-test.json");
        String original = "{\"General\": {\"enabled\": true, \"count\": 10, \"scale\": 0.25, \"names\": [\"a\", \"b\"], "
                + "\"legacyOption\": 1}, \"Other\": {\"other\": 5}}";
        Files.write(file, (original).getBytes(StandardCharsets.UTF_8));

        ConfigSpec.LoadResult directResult = config.spec.load(original);
        assertEquals(Arrays.asList("General.legacyOption"), directResult.unknownKeys());
        assertTrue(directResult.needsCorrection());

        ConfigFile.load(config.spec, dir, LOGGER);

        assertFalse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8).contains("legacyOption"));

        Path backup = dir.resolve("sophisticatedbuilding-test.json.bak");
        assertTrue(Files.exists(backup));
        assertTrue(new String(Files.readAllBytes(backup), StandardCharsets.UTF_8).contains("legacyOption"));

        // Loading the corrected file again reports no more unknown keys.
        ConfigSpec.LoadResult secondResult = config.spec.load(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        assertTrue(secondResult.unknownKeys().isEmpty());
        assertFalse(secondResult.needsCorrection());
    }

    @Test
    void secondBackupUsesNumberedSuffixWhenFirstAlreadyExists(@TempDir Path dir) throws IOException {
        TestConfig config = new TestConfig();
        Path file = dir.resolve("sophisticatedbuilding-test.json");
        Files.write(file, ("{\"General\": {\"enabled\": true, \"count\": 500, \"scale\": 0.25, \"names\": [\"a\", \"b\"]}, "
                + "\"Other\": {\"other\": 5}}").getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("sophisticatedbuilding-test.json.bak"), ("existing backup").getBytes(StandardCharsets.UTF_8));

        ConfigFile.load(config.spec, dir, LOGGER);

        assertTrue(Files.exists(dir.resolve("sophisticatedbuilding-test-1.json.bak")));
        assertEquals("existing backup", new String(Files.readAllBytes(dir.resolve("sophisticatedbuilding-test.json.bak")), StandardCharsets.UTF_8));
    }

    @Test
    void serverConfigContainsSurvivalReplaceAndSyncRoundTrips() {
        try {
            JsonObject file = new JsonParser().parse(serverSpec().toFileJson()).getAsJsonObject();
            assertFalse(file.getAsJsonObject("SurvivalReplace").get("enabled").getAsBoolean());
            assertFalse(ServerConfig.survivalReplace.enabled.get());

            simple(ServerConfig.survivalReplace.enabled).set(true);
            simple(ServerConfig.survivalBreaking.maxDelayTicks).set(99);
            String sync = serverSpec().toSyncJson();
            serverSpec().resetToDefaults();

            ConfigSpec.LoadResult result = serverSpec().loadSync(sync);
            assertTrue(result.parsed());
            assertFalse(result.missingKeys());
            assertTrue(ServerConfig.survivalReplace.enabled.get());
            assertEquals(99, ServerConfig.survivalBreaking.maxDelayTicks.get());
            assertFalse(sync.contains("_comment"));
        } finally {
            serverSpec().resetToDefaults();
        }
    }

    @Test
    void whitelistIsNeverSyncedAndKeepsLocalValue() {
        try {
            simple(ServerConfig.validation.whitelist).set(Arrays.asList("SecretAdmin"));
            String sync = serverSpec().toSyncJson();

            JsonObject validation = new JsonParser().parse(sync).getAsJsonObject().getAsJsonObject("Validation");
            assertFalse(validation.has("whitelist"));
            assertTrue(validation.has("allowInSurvival"));
            assertFalse(sync.contains("SecretAdmin"));
            assertFalse(sync.contains("Player1"));
            assertTrue(serverSpec().toFileJson().contains("SecretAdmin"));

            simple(ServerConfig.validation.whitelist).set(Arrays.asList("LocalName"));
            assertTrue(serverSpec().loadSync(sync).parsed());
            assertEquals(Arrays.asList("LocalName"), ServerConfig.validation.whitelist.get());

            assertFalse(serverSpec().loadSync("{broken").parsed());
            assertEquals(Arrays.asList("LocalName"), ServerConfig.validation.whitelist.get());
        } finally {
            serverSpec().resetToDefaults();
        }
    }

    @Test
    void setAndSaveWriteTheFileThatTheNextLoadReads(@TempDir Path dir) {
        TestConfig config = new TestConfig();
        ConfigFile.load(config.spec, dir, LOGGER);

        // What the player settings screen does: set in memory, then save
        config.enabled.set(false);
        config.scale.set(0.5);
        ConfigFile.save(config.spec, dir, LOGGER);

        TestConfig reloaded = new TestConfig();
        ConfigFile.load(reloaded.spec, dir, LOGGER);
        assertFalse(reloaded.enabled.get());
        assertEquals(0.5, reloaded.scale.get());
        assertEquals(10, reloaded.count.get());
    }

    @Test
    void numberValuesExposeTheirRangeAndDefault() {
        TestConfig config = new TestConfig();
        NumberConfigValue<Integer> count = (NumberConfigValue<Integer>) (ConfigValue<Integer>) config.count;
        assertEquals(1, count.getMin());
        assertEquals(100, count.getMax());
        assertEquals(10, count.getDefault());
        NumberConfigValue<Double> clientScale = ClientConfig.visuals.previewScale;
        assertEquals(0.05, clientScale.getMin());
        assertEquals(1.0, clientScale.getMax());
        assertEquals(0.25, clientScale.getDefault());
    }
}
