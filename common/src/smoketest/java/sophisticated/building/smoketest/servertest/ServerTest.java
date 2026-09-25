package sophisticated.building.smoketest.servertest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a server test method ({@code void name(ServerTestHelper helper)}), the counterpart of vanilla's
 * {@code @GameTest} of Minecraft 1.17+, which Minecraft 1.16.5 ships without. The tests of a class run one after the
 * other in an empty area of a dedicated server, see {@link ServerTestRunner}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ServerTest {

    /** Name of the group the test belongs to (the game test batch on the newer branches); informational only. */
    String batch() default "default";

    /** Ticks the test may take before it fails (vanilla's default is 100). */
    int timeoutTicks() default 100;

    boolean required() default true;
}
