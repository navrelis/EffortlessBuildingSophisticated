package sophisticated.building.smoketest.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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
import sophisticated.building.platform.ClientServices;
import sophisticated.building.smoketest.SmokeReport;
import sophisticated.building.smoketest.SmokeTest;

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
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
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
                if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw new RuntimeException(cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        String screen = String.valueOf(mc.screen == null ? null : mc.screen.getClass().getName());
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
            player.abilities.flying = false;
            player.onUpdateAbilities();
            player.connection.teleport(feet.x, feet.y, feet.z, player.yRot, player.xRot);
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
        player.yRot = yaw;
        player.xRot = pitch;
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        player.yBodyRot = yaw;
    }

    /** Selects the hotbar slot on the client (the server learns it from the client, as for a scroll or number key). */
    public void selectHotbarSlot(int slot) {
        clientRun(() -> mc.player.inventory.selected = slot);
        waitUntilServer("the server to see hotbar slot " + slot, 40, server -> serverPlayer(server).inventory.selected == slot);
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
        serverRun(server -> server.getCommands().performCommand(server.createCommandSourceStack(), command));
    }

    //endregion

    //region World helpers (server side)

    public static int count(Inventory inventory, Item item) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if ((stack.getItem() == item)) total += stack.getCount();
        }
        return total;
    }

    public BlockState serverBlock(BlockPos pos) {
        return server(server -> overworld(server).getBlockState(pos));
    }

    //endregion

    //region Screenshots

    /** Saves the last rendered frame as {@code <out>/screenshots/<name>.png} and lists it in the result. */
    public void screenshot(String name) {
        clientRun(() -> mc.getToasts().clear());
        waitTicks(2);
        Path file = SmokeTest.outDir().resolve("screenshots").resolve(name + ".png");
        clientRun(() -> {
            try {
                Files.createDirectories(file.getParent());
                try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    image.writeToFile(file);
                }
            } catch (Exception e) {
                throw new RuntimeException("Could not save screenshot " + file, e);
            }
        });
        SmokeReport.get().addScreenshot(file);
    }

    //endregion
}
