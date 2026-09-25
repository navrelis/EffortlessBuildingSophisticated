package sophisticated.building.smoketest.servertest;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Steps of a server test that run over several ticks (the counterpart of vanilla's {@code GameTestSequence}): every
 * tick the steps run in order until one throws a {@link ServerTestAssertException}, which is retried on the next tick;
 * any other exception fails the test.
 */
public final class ServerTestSequence {

    private final ServerTestHelper helper;
    private final Deque<Step> steps = new ArrayDeque<>();

    ServerTestSequence(ServerTestHelper helper) {
        this.helper = helper;
    }

    /** Runs the check every tick until it no longer throws a {@link ServerTestAssertException}. */
    public ServerTestSequence thenWaitUntil(Runnable check) {
        steps.add(tick -> check.run());
        return this;
    }

    /** Waits the given number of ticks. */
    public ServerTestSequence thenIdle(int ticks) {
        long[] until = {-1};
        steps.add(tick -> {
            if (until[0] < 0) {
                until[0] = tick + ticks;
            }
            if (tick < until[0]) {
                throw new ServerTestAssertException("Waiting " + ticks + " ticks");
            }
        });
        return this;
    }

    /** Runs the action once (retried on the next tick while it throws a {@link ServerTestAssertException}). */
    public ServerTestSequence thenExecute(Runnable action) {
        steps.add(tick -> action.run());
        return this;
    }

    public void thenSucceed() {
        steps.add(tick -> helper.succeed());
    }

    /** Runs the pending steps; a waiting step ends the tick with its {@link ServerTestAssertException}. */
    void tick(long tick) {
        while (!steps.isEmpty()) {
            steps.peekFirst().run(tick);
            steps.pollFirst();
        }
    }

    private interface Step {
        void run(long tick);
    }
}
