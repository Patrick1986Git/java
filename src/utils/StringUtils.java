package utils;

/**
 * Prosty util do operacji na Stringach.
 */
public final class StringUtils {
    private StringUtils() {}

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String safe(String s) {
        return s == null ? "" : s;
    }
}