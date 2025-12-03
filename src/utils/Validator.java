package utils;

/**
 * Proste validacje używane w serwisach i GUI.
 */
public final class Validator {
    private Validator() {}

    public static void requireNonNull(Object o, String name) {
        if (o == null) throw new IllegalArgumentException(name + " cannot be null");
    }

    public static void requireNonBlank(String s, String name) {
        if (StringUtils.isBlank(s)) throw new IllegalArgumentException(name + " cannot be blank");
    }
}