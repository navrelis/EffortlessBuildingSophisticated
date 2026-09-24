package sophisticated.building.smoketest;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Records every log event that points at the mod going wrong: ERROR/FATAL lines of the mod's own loggers, and any
 * WARN-or-worse line carrying an exception with a stack frame in the mod's code (e.g. "Failed to handle packet"
 * logged by the game with the mod's exception attached). The harness's own logger is ignored.
 */
public final class ModErrorLogCapture {

    private static final String MOD_PACKAGE = "sophisticated.building.";
    private static final String HARNESS_PACKAGE = "sophisticated.building.smoketest.";
    private static final int MAX_KEPT = 20;

    private static final List<String> EVENTS = new ArrayList<>();
    private static int total;
    private static boolean installed;

    private ModErrorLogCapture() {
    }

    public static synchronized void install() {
        if (installed) return;
        installed = true;
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();
        Appender appender = new CaptureAppender();
        appender.start();
        config.addAppender(appender);
        config.getRootLogger().addAppender(appender, Level.WARN, null);
        // Loggers with additivity=false never reach the root logger
        for (LoggerConfig loggerConfig : config.getLoggers().values()) {
            if (loggerConfig != config.getRootLogger()) {
                loggerConfig.addAppender(appender, Level.WARN, null);
            }
        }
        context.updateLoggers();
    }

    public static synchronized int count() {
        return total;
    }

    public static synchronized List<String> events() {
        return List.copyOf(EVENTS);
    }

    /** Records the {@code <prefix>.no_mod_errors} check. */
    public static void report(String checkName) {
        int count = count();
        if (count == 0) {
            SmokeReport.get().pass(checkName, "No ERROR log lines from the mod and no exceptions thrown from its code");
        } else {
            SmokeReport.get().fail(checkName, count + " error log event(s) from the mod, first ones: " + String.join(" | ", events()));
        }
    }

    static boolean isModProblem(LogEvent event) {
        String loggerName = event.getLoggerName() == null ? "" : event.getLoggerName();
        if (loggerName.startsWith("sophisticatedbuilding.smoketest") || loggerName.startsWith(HARNESS_PACKAGE)) {
            return false;
        }
        boolean modLogger = loggerName.startsWith(MOD_PACKAGE) || loggerName.startsWith("sophisticatedbuilding");
        if (modLogger && event.getLevel().isMoreSpecificThan(Level.ERROR)) {
            return true;
        }
        Throwable thrown = event.getThrown();
        return thrown != null && event.getLevel().isMoreSpecificThan(Level.WARN) && hasModFrame(thrown);
    }

    private static boolean hasModFrame(Throwable thrown) {
        for (Throwable t = thrown; t != null; t = t.getCause() == t ? null : t.getCause()) {
            for (StackTraceElement frame : t.getStackTrace()) {
                String className = frame.getClassName();
                if (className.startsWith(MOD_PACKAGE) && !className.startsWith(HARNESS_PACKAGE)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class CaptureAppender extends AbstractAppender {
        CaptureAppender() {
            super("SophisticatedBuildingSmokeCapture", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            if (!isModProblem(event)) return;
            String line = event.getLevel() + " [" + event.getLoggerName() + "] " + event.getMessage().getFormattedMessage()
                    + (event.getThrown() != null ? " (" + event.getThrown() + ")" : "");
            synchronized (ModErrorLogCapture.class) {
                total++;
                if (EVENTS.size() < MAX_KEPT) EVENTS.add(line);
            }
        }
    }
}
