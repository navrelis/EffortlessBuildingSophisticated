package sophisticated.building.smoketest.servertest;

/**
 * A failed expectation of a server test (the counterpart of vanilla's {@code GameTestAssertException}): while the test
 * has time left, the step that threw it is retried on the next tick; at the timeout it becomes the failure.
 */
public class ServerTestAssertException extends RuntimeException {
    public ServerTestAssertException(String message) {
        super(message);
    }
}
