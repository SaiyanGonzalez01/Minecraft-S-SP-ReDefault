package net.lax1dude.eaglercraft.sp;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;

public class SysUtil {

	private static final JSObject steadyTimeFunc = getSteadyTimeFunc();

	@JSBody(params = { }, script = "return ((typeof performance !== \"undefined\") && (typeof performance.now === \"function\"))"
			+ "? performance.now.bind(performance)"
			+ ": (function(epochStart){ return function() { return Date.now() - epochStart; }; })(Date.now());")
	private static native JSObject getSteadyTimeFunc();

	@JSBody(params = { "steadyTimeFunc" }, script = "return steadyTimeFunc();")
	private static native double steadyTimeMillis0(JSObject steadyTimeFunc);

	public static long steadyTimeMillis() {
		return (long)steadyTimeMillis0(steadyTimeFunc);
	}

	public static long nanoTime() {
		return (long)(steadyTimeMillis0(steadyTimeFunc) * 1000000.0);
	}

	@Async
	public static native void sleep(int millis);

	private static void sleep(int millis, final AsyncCallback<Void> callback) {
		Platform.schedule(new DumbSleepHandler(callback), millis);
	}

	private static class DumbSleepHandler implements PlatformRunnable {
		private final AsyncCallback<Void> callback;
		private DumbSleepHandler(AsyncCallback<Void> callback) {
			this.callback = callback;
		}
		@Override
		public void run() {
			callback.complete(null);
		}
	}

}
