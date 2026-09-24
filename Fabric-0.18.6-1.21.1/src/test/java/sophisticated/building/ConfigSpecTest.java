package sophisticated.building;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sophisticated.building.config.ConfigFile;
import sophisticated.building.config.ConfigSpec;
import sophisticated.building.config.SimpleConfigValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSpecTest {

    private static final Logger LOGGER = LogManager.getLogger(ConfigSpecTest.class);

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
            names = builder.defineList("names", List.of("a", "b"));
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
        assertEquals(List.of("a", "b"), config.names.get());
        JsonObject general = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("General");
        assertEquals("Count. Range: 1 ~ 100. Default: 10", general.get("_comment_count").getAsString());
        assertEquals("Second section.", JsonParser.parseString(json).getAsJsonObject()
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
        assertEquals(List.of("x"), config.names.get());
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
        assertEquals(List.of("a", "b"), config.names.get());
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
        assertEquals(config.spec.toFileJson().strip(), Files.readString(file).strip());

        Files.writeString(file, "{\"General\": {\"count\": 42}}");
        ConfigFile.load(config.spec, dir, LOGGER);
        assertEquals(42, config.count.get());
        JsonObject rewritten = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(42, rewritten.getAsJsonObject("General").get("count").getAsInt());
        assertEquals(5, rewritten.getAsJsonObject("Other").get("other").getAsInt());

        String broken = "{\"General\": {\"count\": 50";
        Files.writeString(file, broken);
        ConfigFile.load(config.spec, dir, LOGGER);
        assertEquals(10, config.count.get());
        assertEquals(broken, Files.readString(file));
    }

    @Test
    void serverConfigContainsSurvivalReplaceAndSyncRoundTrips() {
        try {
            JsonObject file = JsonParser.parseString(ServerConfig.spec.toFileJson()).getAsJsonObject();
            assertFalse(file.getAsJsonObject("SurvivalReplace").get("enabled").getAsBoolean());
            assertFalse(ServerConfig.survivalReplace.enabled.get());

            ServerConfig.survivalReplace.enabled.set(true);
            ServerConfig.survivalBreaking.maxDelayTicks.set(99);
            String sync = ServerConfig.spec.toSyncJson();
            ServerConfig.spec.resetToDefaults();

            ConfigSpec.LoadResult result = ServerConfig.spec.loadSync(sync);
            assertTrue(result.parsed());
            assertFalse(result.missingKeys());
            assertTrue(ServerConfig.survivalReplace.enabled.get());
            assertEquals(99, ServerConfig.survivalBreaking.maxDelayTicks.get());
            assertFalse(sync.contains("_comment"));
        } finally {
            ServerConfig.spec.resetToDefaults();
        }
    }

    @Test
    void whitelistIsNeverSyncedAndKeepsLocalValue() {
        try {
            ServerConfig.validation.whitelist.set(List.of("SecretAdmin"));
            String sync = ServerConfig.spec.toSyncJson();

            JsonObject validation = JsonParser.parseString(sync).getAsJsonObject().getAsJsonObject("Validation");
            assertFalse(validation.has("whitelist"));
            assertTrue(validation.has("allowInSurvival"));
            assertFalse(sync.contains("SecretAdmin"));
            assertFalse(sync.contains("Player1"));
            assertTrue(ServerConfig.spec.toFileJson().contains("SecretAdmin"));

            ServerConfig.validation.whitelist.set(List.of("LocalName"));
            assertTrue(ServerConfig.spec.loadSync(sync).parsed());
            assertEquals(List.of("LocalName"), ServerConfig.validation.whitelist.get());

            assertFalse(ServerConfig.spec.loadSync("{broken").parsed());
            assertEquals(List.of("LocalName"), ServerConfig.validation.whitelist.get());
        } finally {
            ServerConfig.spec.resetToDefaults();
        }
    }
}
