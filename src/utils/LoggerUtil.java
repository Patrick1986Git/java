package utils;

import utils.concurrent.AppExecutors;
import security.AuthManager;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.logging.*;

/**
 * Enterprise-like Logger utility.
 *
 * - zapisuje logi do konsoli oraz do pliku logs/app-%g.log (rotacja po
 * rozmiarze) - asynchroniczne zapisywanie przez
 * AppExecutors.BACKGROUND_EXECUTOR - custom formatter: timestamp, level,
 * thread, user (jeżeli zalogowany), message
 *
 * Jeśli później chcesz rotację "dzienną" lub zaawansowane polityki — polecam
 * Logback/SLF4J.
 */
public final class LoggerUtil {
	private static final Logger LOG = Logger.getLogger("enterprise.app");
	private static final String LOG_DIR = "logs";
	private static final String LOG_PATTERN = LOG_DIR + File.separator + "app-%g.log";
	// limit 10MB per file, keep 7 rotating files
	private static final int LIMIT = 10 * 1024 * 1024;
	private static final int COUNT = 7;
	private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
			.withZone(ZoneId.systemDefault());

	static {
		configure();
	}

	private LoggerUtil() {
	}

	private static void configure() {
		try {
			LOG.setUseParentHandlers(false); // wyłącz domyślny handler żeby mieć pełną kontrolę

			// 1) Console handler (kolor/format prosty)
			ConsoleHandler ch = new ConsoleHandler();
			ch.setLevel(Level.ALL);
			ch.setFormatter(new ContextFormatter());
			LOG.addHandler(ch);

			// 2) Ensure logs directory exists
			File logDir = new File(LOG_DIR);
			if (!logDir.exists()) {
				boolean ok = logDir.mkdirs();
				if (!ok) {
					// jeżeli nie uda się utworzyć katalogu, logujemy do konsoli i kontynuujemy
					System.err.println("LoggerUtil: failed to create logs dir: " + logDir.getAbsolutePath());
				}
			}

			// 3) File handler with rotation by size
			FileHandler fh = new FileHandler(LOG_PATTERN, LIMIT, COUNT, true);
			fh.setLevel(Level.ALL);
			fh.setFormatter(new ContextFormatter());
			LOG.addHandler(fh);

			// 4) Ustaw poziom globalny (możesz zmienić na INFO w produkcji)
			LOG.setLevel(Level.ALL);

			// a small startup message
			LOG.info("LoggerUtil initialized. Logs directory: " + logDir.getAbsolutePath());
		} catch (IOException e) {
			// Jeżeli FileHandler nie zadziała — wypisz do stderr i zostaw konsolę
			System.err.println("LoggerUtil configuration failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Formatter który dodaje timestamp, level, thread i user (jeżeli dostępny)
	private static class ContextFormatter extends Formatter {
		@Override
		public String format(LogRecord record) {
			String ts = TS_FORMATTER.format(Instant.ofEpochMilli(record.getMillis()));
			String level = record.getLevel().getName();
			String thread = Thread.currentThread().getName();
			String user = getCurrentUsername().orElse("-");
			String msg = formatMessage(record);
			String thrown = "";
			if (record.getThrown() != null) {
				StringBuilder sb = new StringBuilder();
				Throwable t = record.getThrown();
				sb.append("\n");
				for (StackTraceElement el : t.getStackTrace()) {
					sb.append("\tat ").append(el.toString()).append("\n");
				}
				thrown = sb.toString();
			}
			return String.format("%s %-7s [%s] user=%s - %s%s%n", ts, level, thread, user, msg, thrown);
		}
	}

	private static java.util.Optional<String> getCurrentUsername() {
		try {
			return AuthManager.get().getCurrentUser().map(u -> u.getUsername());
		} catch (Exception e) {
			return java.util.Optional.empty();
		}
	}

	// core async log method
	private static void asyncLog(Level level, String msg, Throwable t) {
		// use background executor to avoid blocking caller threads
		AppExecutors.BACKGROUND_EXECUTOR.submit(() -> {
			if (t != null) {
				LOG.log(level, msg, t);
			} else {
				LOG.log(level, msg);
			}
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

	public static java.util.concurrent.CompletableFuture<Void> logAsync(Level level, String msg) {
		java.util.concurrent.CompletableFuture<Void> f = new java.util.concurrent.CompletableFuture<>();
		AppExecutors.BACKGROUND_EXECUTOR.submit(() -> {
			try {
				LOG.log(level, msg);
				f.complete(null);
			} catch (Throwable e) {
				f.completeExceptionally(e);
			}
		});
		return f;
	}
}
