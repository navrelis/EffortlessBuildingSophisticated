package sophisticated.building.smoketest.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import sophisticated.building.platform.ClientServices;
import sophisticated.building.smoketest.SmokeReport;
import sophisticated.building.smoketest.SmokeTest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Drives the real client from the harness thread: every game interaction is submitted to the client thread (or to
 * the integrated server thread for test setup and server-side assertions) and waited for, so scenarios read as
 * linear scripts. Waiting is measured in client ticks, counted by the loader glue through {@link #onClientTickEnd()}.
 */
public final class ClientDriver {

    private static final Object TICK_LOCK = new Object();
    private static long ticks;

    private static final long CALL_TIMEOUT_SECONDS = 60;

    final Minecraft mc = Minecraft.getInstance();

    /** Called by the loader glue at the end of every client tick. */
    public static void onClientTickEnd() {
        synchronized (TICK_LOCK) {
            ticks++;
            TICK_LOCK.notifyAll();
        }
    }

    static long tickCount() {
        synchronized (TICK_LOCK) {
            return ticks;
        }
    }

    //region Threads

    /** Runs on the client thread and returns the result. */
    public <T> T client(Supplier<T> action) {
        return await(mc.submit(action), "client task");
    }

    public void clientRun(Runnable action) {
        client(() -> {
            action.run();
            return null;
        });
    }

    /** Runs on the integrated server thread and returns the result. */
    public <T> T server(Function<MinecraftServer, T> action) {
        MinecraftServer server = client(mc::getSingleplayerServer);
        if (server == null) throw new AssertionError("No integrated server is running");
        return await(server.submit(() -> action.apply(server)), "server task");
    }

    public void serverRun(java.util.function.Consumer<MinecraftServer> action) {
        server(server -> {
            action.accept(server);
            return null;
        });
    }

    private static <T> T await(CompletableFuture<T> future, String what) {
        try {
            return future.get(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new RuntimeException(cause);
        } catch (TimeoutException e) {
            throw new AssertionError("The " + what + " did not complete within " + CALL_TIMEOUT_SECONDS + " s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    //endregion

    //region Waiting

    public void waitTicks(int count) {
        long target = tickCount() + count;
        waitForTick(target, count * 50L + 20_000L);
    }

    private static void waitForTick(long target, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        synchronized (TICK_LOCK) {
            while (ticks < target) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) throw new AssertionError("The client stopped ticking (waited for tick " + target + ", at " + ticks + ")");
                try {
                    TICK_LOCK.wait(left);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /** Polls the condition on the client thread once per tick. */
    public void waitUntil(String what, int timeoutTicks, BooleanSupplier condition) {
        for (int i = 0; i <= timeoutTicks; i++) {
            if (client(condition::getAsBoolean)) return;
            waitTicks(1);
        }
        throw new AssertionError("Timed out after " + timeoutTicks + " ticks waiting for " + what);
    }

    /** Polls the condition on the server thread once per client tick. */
    public void waitUntilServer(String what, int timeoutTicks, Predicate<MinecraftServer> condition) {
        for (int i = 0; i <= timeoutTicks; i++) {
            if (server(condition::test)) return;
            waitTicks(1);
        }
        throw new AssertionError("Timed out after " + timeoutTicks + " ticks waiting for " + what);
    }

    /**
     * Polls the condition on the client thread in real time, for phases without client ticks (resource loading,
     * world creation, where the client thread may not run submitted tasks for a long time).
     */
    public void waitUntilRealtime(String what, int timeoutSeconds, BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        CompletableFuture<Boolean> pending = null;
        while (System.currentTimeMillis() < deadline) {
            if (pending == null) pending = mc.submit(condition::getAsBoolean);
            try {
                boolean met = pending.get(250, TimeUnit.MILLISECONDS);
                if (met) return;
                pending = null;
                sleep(250);
            } catch (TimeoutException busy) {
                // The client thread is busy (loading); keep the task queued
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtime) throw runtime;
                if (cause instanceof Error error) throw error;
                throw new RuntimeException(cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        String screen = String.valueOf(mc.gui.screen() == null ? null : mc.gui.screen().getClass().getName());
        throw new AssertionError("Timed out after " + timeoutSeconds + " s waiting for " + what + " (screen: " + screen + ")");
    }

    /** Waits in real time (before a world or even the client exists). */
    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    //endregion

    //region Player

    public static ServerPlayer serverPlayer(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().isEmpty()) throw new AssertionError("No player on the integrated server");
        return server.getPlayerList().getPlayers().get(0);
    }

    public static ServerLevel overworld(MinecraftServer server) {
        return server.overworld();
    }

    /** Teleports the player (server side, like /tp) so that it stands at feet, and waits until the client is there. */
    public void teleport(Vec3 feet) {
        serverRun(server -> {
            ServerPlayer player = serverPlayer(server);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            player.connection.teleport(feet.x, feet.y, feet.z, player.getYRot(), player.getXRot());
        });
        waitUntil("the client to arrive at " + feet, 100, () -> mc.player != null && mc.player.position().distanceToSqr(feet) < 0.01);
        waitTicks(2);
    }

    /** Turns the client player's head so that the crosshair points at the target (what moving the mouse does). */
    public void lookAt(Vec3 target) {
        clientRun(() -> aimNow(target));
    }

    /** lookAt on the client thread. */
    void aimNow(Vec3 target) {
        LocalPlayer player = mc.player;
        Vec3 eye = player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F);
        float pitch = Mth.wrapDegrees((float) (-(Mth.atan2(dy, horizontal) * Mth.RAD_TO_DEG)));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        player.yBodyRot = yaw;
    }

    /** Selects the hotbar slot on the client (the server learns it from the client, as for a scroll or number key). */
    public void selectHotbarSlot(int slot) {
        clientRun(() -> mc.player.getInventory().setSelectedSlot(slot));
        waitUntilServer("the server to see hotbar slot " + slot, 40, server -> serverPlayer(server).getInventory().getSelectedSlot() == slot);
    }

    /** A mouse button press and release of the key mapping, as MouseHandler does for a real click (held one tick). */
    public void click(KeyMapping mapping) {
        InputConstants.Key key = client(() -> ClientServices.CLIENT.getBoundKey(mapping));
        long pressedAt = client(() -> {
            KeyMapping.set(key, true);
            KeyMapping.click(key);
            return tickCount();
        });
        waitForTick(pressedAt + 1, 20_000L);
        clientRun(() -> KeyMapping.set(key, false));
        waitTicks(1);
    }

    public void rightClick() {
        click(mc.options.keyUse);
    }

    public void leftClick() {
        click(mc.options.keyAttack);
    }

    /** Server-side command as the server console (op level 4). */
    public void command(String command) {
        serverRun(server -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    //endregion

    //region World helpers (server side)

    public static int count(Inventory inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    public BlockState serverBlock(BlockPos pos) {
        return server(server -> overworld(server).getBlockState(pos));
    }

    //endregion

    //region Mouse and keyboard in screens

    /**
     * Moves the mouse pointer to GUI coordinates: what the cursor callback does when the player moves the mouse (the
     * harness detached it, see ClientWindow). Screens see the pointer from their next frame on (hover state, tooltips).
     */
    public void pointAt(double guiX, double guiY) {
        clientRun(() -> pointNow(guiX, guiY));
    }

    private void pointNow(double guiX, double guiY) {
        com.mojang.blaze3d.platform.Window window = mc.getWindow();
        try {
            mouseField("xpos").setDouble(mc.mouseHandler, guiX * window.getScreenWidth() / window.getGuiScaledWidth());
            mouseField("ypos").setDouble(mc.mouseHandler, guiY * window.getScreenHeight() / window.getGuiScaledHeight());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A left click at GUI coordinates: moves the pointer there, then presses and releases the button through
     * MouseHandler, exactly what GLFW's mouse button callback does (loader screen events included).
     */
    public void clickAt(double guiX, double guiY) {
        clientRun(() -> {
            pointNow(guiX, guiY);
            mouseButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS);
        });
        clientRun(() -> mouseButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE));
    }

    /** Turns the mouse wheel (positive = away from the player) with the pointer at GUI coordinates, through MouseHandler. */
    public void scrollAt(double guiX, double guiY, double amount) {
        clientRun(() -> {
            pointNow(guiX, guiY);
            try {
                method(MouseHandler.class, "onScroll", long.class, double.class, double.class).invoke(mc.mouseHandler, mc.getWindow().handle(), 0.0, amount);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("MouseHandler#onScroll failed", e);
            }
        });
    }

    /**
     * A left-button drag in a screen: press at the start (MouseHandler), move the pointer to the end and hand the
     * screen the move and drag events, release at the end (MouseHandler). The move/drag step calls the screen the way
     * {@code MouseHandler#handleAccumulatedMovement} does; that method itself only runs for the focused window, and the
     * harness window is never focused (ClientWindow).
     */
    public void dragTo(double fromX, double fromY, double toX, double toY) {
        clientRun(() -> {
            pointNow(fromX, fromY);
            mouseButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS);
        });
        clientRun(() -> {
            pointNow(toX, toY);
            if (mc.gui.screen() != null) {
                mc.gui.screen().mouseMoved(toX, toY);
                mc.gui.screen().mouseDragged(new MouseButtonEvent(toX, toY, new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)), toX - fromX, toY - fromY);
            }
        });
        clientRun(() -> mouseButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE));
    }

    /**
     * Presses and releases a keyboard key through KeyboardHandler, what GLFW's key callback does (private since the
     * 1.21.9 input records: {@code keyPress(long window, int action, KeyEvent)}).
     */
    public void pressKey(int glfwKey) {
        KeyEvent event = new KeyEvent(glfwKey, GLFW.glfwGetKeyScancode(glfwKey), 0);
        clientRun(() -> keyboardKey(GLFW.GLFW_PRESS, event));
        clientRun(() -> keyboardKey(GLFW.GLFW_RELEASE, event));
    }

    private void keyboardKey(int action, KeyEvent event) {
        try {
            method(KeyboardHandler.class, "keyPress", long.class, int.class, KeyEvent.class).invoke(mc.keyboardHandler, mc.getWindow().handle(), action, event);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("KeyboardHandler#keyPress failed", e);
        }
    }

    /** {@code MouseHandler#onButton(long window, MouseButtonInfo, int action)} (1.21.9+; {@code onPress} before). */
    private void mouseButton(int button, int action) {
        try {
            method(MouseHandler.class, "onButton", long.class, MouseButtonInfo.class, int.class)
                    .invoke(mc.mouseHandler, mc.getWindow().handle(), new MouseButtonInfo(button, 0), action);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("MouseHandler#onButton failed", e);
        }
    }

    private static Field mouseField(String name) {
        try {
            Field field = MouseHandler.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new AssertionError("MouseHandler has no field " + name + " (the harness moves the pointer through it)", e);
        }
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new AssertionError(owner.getSimpleName() + " has no method " + name + " (the harness clicks, scrolls and presses keys through it)", e);
        }
    }

    //endregion

    //region Screenshots

    /** Saves the last rendered frame as {@code <out>/screenshots/<name>.png} and lists it in the result. */
    public void screenshot(String name) {
        clientRun(() -> mc.gui.toastManager().clear());
        waitTicks(2);
        Path file = SmokeTest.outDir().resolve("screenshots").resolve(name + ".png");
        // 1.21.5+: the frame is read back from the GPU asynchronously; the callback owns (and closes) the image
        CompletableFuture<Void> written = new CompletableFuture<>();
        clientRun(() -> {
            try {
                Files.createDirectories(file.getParent());
            } catch (Exception e) {
                throw new RuntimeException("Could not create " + file.getParent(), e);
            }
            Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), image -> {
                try (NativeImage owned = image) {
                    owned.writeToFile(file);
                    written.complete(null);
                } catch (Exception e) {
                    written.completeExceptionally(new RuntimeException("Could not save screenshot " + file, e));
                }
            });
        });
        await(written, "screenshot " + name);
        SmokeReport.get().addScreenshot(file);
    }

    //endregion
}
