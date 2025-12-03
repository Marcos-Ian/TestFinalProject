package util;

public final class ValidationUtils {
    private ValidationUtils() {}

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
