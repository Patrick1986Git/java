package utils;

import utils.concurrent.AppExecutors;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asynchroniczny wrapper nad java.util.logging. Wysyła logi w tle przez
 * AppExecutors.BACKGROUND_EXECUTOR.
 */
public final class LoggerUtil {
	private static final Logger LOG = Logger.getLogger("enterprise.app");

	private LoggerUtil() {
	}

	private static void asyncLog(Level level, String msg, Throwable t) {
		AppExecutors.BACKGROUND_EXECUTOR.submit(() -> {
			if (t != null)
				LOG.log(level, msg, t);
			else
				LOG.log(level, msg);
		});
	}

	public static void log(Level level, String msg) {
		asyncLog(level, msg, null);
	}

	public static void info(String msg) {
		asyncLog(Level.INFO, msg, null);
	}

	public static void warn(String msg) {
		asyncLog(Level.WARNING, msg, null);
	}

	public static void error(String msg, Throwable t) {
		asyncLog(Level.SEVERE, msg, t);
	}

	public static CompletableFuture<Void> logAsync(Level level, String msg) {
		return CompletableFuture.runAsync(() -> LOG.log(level, msg), AppExecutors.BACKGROUND_EXECUTOR);
	}
}
