package utils;

import utils.concurrent.AppExecutors;
import security.AuthManager;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * Enterprise-like Logger utility with separate audit logger (JSON lines).
 */
public final class LoggerUtil {
	private static final Logger LOG = Logger.getLogger("enterprise.app");
	private static final Logger AUDIT_LOG = Logger.getLogger("enterprise.audit");
	private static final String LOG_DIR = "logs";
	private static final String APP_PATTERN = LOG_DIR + File.separator + "app-%g.log";
	private static final String AUDIT_PATTERN = LOG_DIR + File.separator + "audit-%g.log";
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
			LOG.setUseParentHandlers(false);
			AUDIT_LOG.setUseParentHandlers(false);

			// Console handler for app logger
			ConsoleHandler ch = new ConsoleHandler();
			ch.setLevel(Level.ALL);
			ch.setFormatter(new ContextFormatter());
			LOG.addHandler(ch);

			// Ensure logs dir exists
			File logDir = new File(LOG_DIR);
			if (!logDir.exists()) {
				boolean ok = logDir.mkdirs();
				if (!ok) {
					System.err.println("LoggerUtil: failed to create logs dir: " + logDir.getAbsolutePath());
				}
			}

			// File handler for app logger (rotating)
			FileHandler fh = new FileHandler(APP_PATTERN, LIMIT, COUNT, true);
			fh.setLevel(Level.ALL);
			fh.setFormatter(new ContextFormatter());
			LOG.addHandler(fh);
			LOG.setLevel(Level.ALL);

			// File handler for audit logger (rotating) - simple plain text JSON lines
			FileHandler auditFh = new FileHandler(AUDIT_PATTERN, LIMIT, COUNT, true);
			auditFh.setLevel(Level.ALL);
			auditFh.setFormatter(new SimpleFormatter() {
				private final DateTimeFormatter fmt = TS_FORMATTER;

				@Override
				public synchronized String format(LogRecord record) {
					// We will write pre-built JSON in message; preserve newline
					return formatMessage(record) + System.lineSeparator();
				}
			});
			AUDIT_LOG.addHandler(auditFh);
			AUDIT_LOG.setLevel(Level.ALL);

			LOG.info("LoggerUtil initialized. Logs directory: " + logDir.getAbsolutePath());
		} catch (IOException e) {
			System.err.println("LoggerUtil configuration failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Formatter with context (ts, level, thread, user)
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

	private static void asyncLog(Level level, String msg, Throwable t) {
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

	// ========== Audit API ==========

	/**
	 * Zapisywanie audytu jako JSON jednej linii. - action: np. "CREATE_PERSON",
	 * "UPDATE_EMPLOYEE", "DELETE_STUDENT", "CHANGE_PASSWORD" - target: np.
	 * "person:123" albo "user:john" - details: dowolny tekst (można podać krótki
	 * opis/zmiany)
	 */
	public static void audit(String action, String target, String details) {
		String ts = TS_FORMATTER.format(Instant.now());
		String user = getCurrentUsername().orElse("system");
		String thread = Thread.currentThread().getName();
		String json = buildJsonLine(ts, user, thread, action, target, details);
		// audit should be append-only and fast: we still submit to background executor
		AppExecutors.BACKGROUND_EXECUTOR.submit(() -> {
			// put into AUDIT_LOG as INFO — SimpleFormatter will just print message
			AUDIT_LOG.info(json);
		});
	}

	private static String buildJsonLine(String ts, String user, String thread, String action, String target,
			String details) {
		return "{" + "\"ts\":\"" + escapeJson(ts) + "\"," + "\"user\":\"" + escapeJson(user) + "\"," + "\"thread\":\""
				+ escapeJson(thread) + "\"," + "\"action\":\"" + escapeJson(action) + "\"," + "\"target\":\""
				+ escapeJson(target) + "\"," + "\"details\":\"" + escapeJson(details) + "\"" + "}";
	}

	private static String escapeJson(String s) {
		if (s == null)
			return "";
		StringBuilder sb = new StringBuilder(s.length() + 20);
		for (char c : s.toCharArray()) {
			switch (c) {
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				if (c < 0x20) {
					sb.append(String.format("\\u%04x", (int) c));
				} else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	// Optional async variant returning future
	public static java.util.concurrent.CompletableFuture<Void> auditAsync(String action, String target,
			String details) {
		java.util.concurrent.CompletableFuture<Void> f = new java.util.concurrent.CompletableFuture<>();
		AppExecutors.BACKGROUND_EXECUTOR.submit(() -> {
			try {
				audit(action, target, details);
				f.complete(null);
			} catch (Throwable e) {
				f.completeExceptionally(e);
			}
		});
		return f;
	}

	// kept for compatibility
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
