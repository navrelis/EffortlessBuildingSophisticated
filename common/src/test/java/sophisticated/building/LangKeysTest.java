package sophisticated.building;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every translation key written out in the mod's code exists in en_us.json: string literals that are whole keys of the
 * mod ({@code sophisticatedbuilding.*}, {@code key.sophisticatedbuilding.*}, {@code item.sophisticatedbuilding.*}) and
 * {@code Lang.translateDirect("x")} keys (prefixed with the mod id). Keys built at runtime (build mode and action names)
 * are not covered. Scans ../common/src/main/java and the loader's own src/main/java (the test runs in a loader folder).
 */
class LangKeysTest {

    private static final Pattern FULL_KEY = Pattern.compile("\"((?:key\\.|item\\.)?sophisticatedbuilding\\.[a-z0-9_.]*[a-z0-9_])\"");
    private static final Pattern TRANSLATE_DIRECT = Pattern.compile("translateDirect\\(\"([a-zA-Z0-9_.]+)\"");
    private static final Pattern LANG_ENTRY = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:", Pattern.MULTILINE);

    private static Set<String> langKeys() throws IOException {
        try (InputStream in = LangKeysTest.class.getResourceAsStream("/assets/sophisticatedbuilding/lang/en_us.json")) {
            assertNotNull(in, "en_us.json is not on the test classpath");
            String json = new String(readAll(in), StandardCharsets.UTF_8);
            Set<String> keys = new HashSet<>();
            Matcher matcher = LANG_ENTRY.matcher(json);
            while (matcher.find()) keys.add(matcher.group(1));
            return keys;
        }
    }

    // InputStream#readAllBytes is Java 9+
    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    private static List<Path> sources() throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path root : Arrays.asList(Paths.get("../common/src/main/java"), Paths.get("src/main/java"))) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
            }
        }
        return files;
    }

    @Test
    void everyKeyInTheCodeExistsInEnUs() throws IOException {
        Set<String> lang = langKeys();
        List<Path> files = sources();
        assertFalse(files.isEmpty(), "no sources found from " + Paths.get("").toAbsolutePath());
        Set<String> missing = new TreeSet<>();
        int checked = 0;
        for (Path file : files) {
            String code = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Matcher full = FULL_KEY.matcher(code);
            while (full.find()) {
                String key = full.group(1);
                if (key.endsWith(".json") || key.startsWith("sophisticatedbuilding.smoketest")) continue;
                checked++;
                if (!lang.contains(key)) missing.add(key + " (" + file.getFileName() + ")");
            }
            Matcher direct = TRANSLATE_DIRECT.matcher(code);
            while (direct.find()) {
                checked++;
                String key = "sophisticatedbuilding." + direct.group(1);
                if (!lang.contains(key)) missing.add(key + " (" + file.getFileName() + ")");
            }
        }
        assertTrue(checked > 50, "only " + checked + " keys found in the code");
        assertTrue(missing.isEmpty(), "Keys used in the code but missing in en_us.json: " + missing);
    }
}
