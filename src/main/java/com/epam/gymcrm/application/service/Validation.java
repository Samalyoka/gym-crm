package com.epam.gymcrm.application.service;

import com.epam.gymcrm.domain.model.User;

/**
 * Checks that required fields are filled before create/update.
 */
final class Validation {

    private Validation() {
    }

    static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    static <T> T requireNotNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    static void requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    static void requireUser(User user) {
        requireNotNull(user, "user");
        requireText(user.firstName(), "firstName");
        requireText(user.lastName(), "lastName");
    }
}
