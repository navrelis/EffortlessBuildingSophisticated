package sophisticated.building.utilities;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Resolves a public instance method by name and parameter types only, ignoring its declared return
 * type, and hands back a {@link MethodHandle} for it. This lets a caller link against a method
 * whose return type changed upstream (e.g. {@code void -> boolean}) without a
 * {@code NoSuchMethodError}: invoking the handle as a statement adapts the return type to
 * {@code void} per JLS 15.12.3.
 *
 * <p>No Minecraft or Sophisticated imports; testable in isolation like {@code ToolSelector}.
 */
public final class ReturnTypeAgnosticInvoker {

    private ReturnTypeAgnosticInvoker() {
    }

    /**
     * Finds the public instance method {@code name(paramTypes...)} on {@code owner} regardless of
     * its declared return type and returns a handle for it, or empty if it does not exist or is
     * not accessible. Never throws.
     */
    public static Optional<MethodHandle> findVirtualIgnoringReturnType(Class<?> owner, String name, Class<?>... paramTypes) {
        try {
            Method method = owner.getMethod(name, paramTypes);
            MethodHandle handle = MethodHandles.publicLookup().unreflect(method);
            return Optional.of(handle);
        } catch (NoSuchMethodException | IllegalAccessException | SecurityException | LinkageError e) {
            return Optional.empty();
        }
    }
}
