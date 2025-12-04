package util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized helper for activity logging.
 * Every entry should include timestamp (from java.util.logging),
 * actor, action, entity type, entity id, and a descriptive message.
 */
public final class ActivityLogger {

    private static final Logger LOGGER = Logger.getLogger(ActivityLogger.class.getName());

    private ActivityLogger() {}

    public static void log(String actor,
                           String action,
                           String entityType,
                           String entityId,
                           String message) {
        if (actor == null || actor.isBlank()) {
            actor = "UNKNOWN";
        }
        if (entityType == null || entityType.isBlank()) {
            entityType = "N/A";
        }
        if (entityId == null || entityId.isBlank()) {
            entityId = "N/A";
        }
        String formatted = String.format(
                "actor=%s | action=%s | entityType=%s | entityId=%s | message=%s",
                actor, action, entityType, entityId, message
        );
        LOGGER.log(Level.INFO, formatted);
    }
}
