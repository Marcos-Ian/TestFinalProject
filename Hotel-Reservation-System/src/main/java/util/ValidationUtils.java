// ============================================================================
// ValidationUtils.java - Utility class for validation
// ============================================================================
package util;

import java.time.LocalDate;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utility class for common validation operations.
 */
public class ValidationUtils {
    private static final Logger LOGGER = Logger.getLogger(ValidationUtils.class.getName());

    // Regex patterns
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\d{10}$");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-z\\s'-]{2,50}$");

    // Private constructor to prevent instantiation
    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Require a condition to be true, throw exception with message if false
     */
    public static void require(boolean condition, String message) {
        if (!condition) {
            LOGGER.warning("Validation failed: " + message);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate phone number (10 digits)
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }
        // Remove common formatting characters
        String cleaned = phone.replaceAll("[\\s()-]", "");
        return PHONE_PATTERN.matcher(cleaned).matches();
    }

    /**
     * Validate name (letters, spaces, hyphens, apostrophes only)
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return NAME_PATTERN.matcher(name.trim()).matches();
    }

    /**
     * Validate date range
     */
    public static boolean isValidDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            return false;
        }

        LocalDate today = LocalDate.now();

        // Check-in cannot be in the past
        if (checkIn.isBefore(today)) {
            return false;
        }

        // Check-out must be after check-in
        return !checkOut.isBefore(checkIn.plusDays(1));
    }

    /**
     * Validate that a string is not null or empty
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Validate that a number is positive
     */
    public static boolean isPositive(double value) {
        return value > 0;
    }

    /**
     * Validate that a number is non-negative
     */
    public static boolean isNonNegative(double value) {
        return value >= 0;
    }

    /**
     * Validate occupancy against capacity
     */
    public static boolean isValidOccupancy(int guests, int capacity) {
        return guests > 0 && guests <= capacity;
    }

    /**
     * Validate discount percentage
     */
    public static boolean isValidDiscount(double discount, double maxDiscount) {
        return discount >= 0 && discount <= maxDiscount;
    }

    /**
     * Sanitize string input (remove leading/trailing whitespace)
     */
    public static String sanitize(String input) {
        return input == null ? "" : input.trim();
    }

    /**
     * Validate and throw exception with custom message
     */
    public static void validateEmail(String email, String fieldName) {
        require(isValidEmail(email),
                fieldName + " must be a valid email address");
    }

    public static void validatePhone(String phone, String fieldName) {
        require(isValidPhone(phone),
                fieldName + " must be a valid 10-digit phone number");
    }

    public static void validateName(String name, String fieldName) {
        require(isValidName(name),
                fieldName + " must contain only letters, spaces, hyphens, and apostrophes (2-50 characters)");
    }

    public static void validateNotEmpty(String value, String fieldName) {
        require(isNotEmpty(value),
                fieldName + " cannot be empty");
    }

    public static void validatePositive(double value, String fieldName) {
        require(isPositive(value),
                fieldName + " must be positive");
    }

    public static void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        require(checkIn != null, "Check-in date is required");
        require(checkOut != null, "Check-out date is required");
        require(!checkIn.isBefore(LocalDate.now()),
                "Check-in date cannot be in the past");
        require(!checkOut.isBefore(checkIn.plusDays(1)),
                "Check-out date must be at least one day after check-in");
    }
}