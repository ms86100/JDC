package com.jira.migration.security;

import java.util.UUID;

/**
 * Propagates migration actor user id to outbound service clients on async worker threads.
 */
public final class MigrationRequestContext {

    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();

    private MigrationRequestContext() {
    }

    public static void setUserId(UUID userId) {
        if (userId != null) {
            USER_ID.set(userId);
        } else {
            USER_ID.remove();
        }
    }

    public static UUID getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
