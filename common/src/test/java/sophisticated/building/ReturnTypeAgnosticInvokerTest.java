package sophisticated.building;

import org.junit.jupiter.api.Test;
import sophisticated.building.utilities.ReturnTypeAgnosticInvoker;

import java.lang.invoke.MethodHandle;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnTypeAgnosticInvokerTest {

	/** Stands in for a Sophisticated Backpacks build &lt;= 3.25.x, where runOnBackpacks returns void. */
	public static final class VoidReturningOwner {
		private boolean called;

		public void runOnBackpacks(Object player, Runnable c) {
			called = true;
			c.run();
		}

		public boolean wasCalled() {
			return called;
		}
	}

	/** Stands in for a Sophisticated Backpacks build &gt;= 3.26.0, where runOnBackpacks returns boolean. */
	public static final class BooleanReturningOwner {
		private boolean called;

		public boolean runOnBackpacks(Object player, Runnable c) {
			called = true;
			c.run();
			return true;
		}

		public boolean wasCalled() {
			return called;
		}
	}

	@Test
	void resolvesTheVoidReturningVariant() {
		Optional<MethodHandle> handle = ReturnTypeAgnosticInvoker.findVirtualIgnoringReturnType(
				VoidReturningOwner.class, "runOnBackpacks", Object.class, Runnable.class);
		assertTrue(handle.isPresent());
	}

	@Test
	void resolvesTheBooleanReturningVariant() {
		Optional<MethodHandle> handle = ReturnTypeAgnosticInvoker.findVirtualIgnoringReturnType(
				BooleanReturningOwner.class, "runOnBackpacks", Object.class, Runnable.class);
		assertTrue(handle.isPresent());
	}

	@Test
	void invokingTheVoidReturningHandleAsAStatementRunsTheBody() throws Throwable {
		Optional<MethodHandle> handle = ReturnTypeAgnosticInvoker.findVirtualIgnoringReturnType(
				VoidReturningOwner.class, "runOnBackpacks", Object.class, Runnable.class);
		assertTrue(handle.isPresent());

		VoidReturningOwner owner = new VoidReturningOwner();
		boolean[] ran = new boolean[1];
		handle.get().invoke(owner, new Object(), (Runnable) () -> ran[0] = true);

		assertTrue(owner.wasCalled());
		assertTrue(ran[0]);
	}

	@Test
	void invokingTheBooleanReturningHandleAsAStatementAdaptsToVoidAndRunsTheBody() throws Throwable {
		Optional<MethodHandle> handle = ReturnTypeAgnosticInvoker.findVirtualIgnoringReturnType(
				BooleanReturningOwner.class, "runOnBackpacks", Object.class, Runnable.class);
		assertTrue(handle.isPresent());

		BooleanReturningOwner owner = new BooleanReturningOwner();
		boolean[] ran = new boolean[1];
		// Statement form (no assignment): call-site return type is void, so asType drops the
		// boolean result per JLS 15.12.3. This is the crux of the boolean -> void adaptation.
		handle.get().invoke(owner, new Object(), (Runnable) () -> ran[0] = true);

		assertTrue(owner.wasCalled());
		assertTrue(ran[0]);
	}

	@Test
	void missingMethodNameYieldsEmpty() {
		Optional<MethodHandle> handle = ReturnTypeAgnosticInvoker.findVirtualIgnoringReturnType(
				VoidReturningOwner.class, "noSuchMethod", Object.class, Runnable.class);
		assertFalse(handle.isPresent());
	}

	@Test
	void wrongParameterTypesYieldEmpty() {
		Optional<MethodHandle> handle = ReturnTypeAgnosticInvoker.findVirtualIgnoringReturnType(
				VoidReturningOwner.class, "runOnBackpacks", String.class, Integer.class);
		assertFalse(handle.isPresent());
	}
}
