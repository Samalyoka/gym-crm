package com.epam.gymcrm.application.service;

import com.epam.gymcrm.domain.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationTest {

    @Test
    void requireText_rejectsNullAndBlank() {
        IllegalArgumentException nullError =
                assertThrows(IllegalArgumentException.class, () -> Validation.requireText(null, "firstName"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireText("  ", "firstName"));

        assertEquals("firstName is required", nullError.getMessage());
    }

    @Test
    void requireText_acceptsText() {
        assertDoesNotThrow(() -> Validation.requireText("John", "firstName"));
    }

    @Test
    void requireNotNull_returnsValueOrThrows() {
        Object value = new Object();

        assertSame(value, Validation.requireNotNull(value, "value"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireNotNull(null, "value"));
    }

    @Test
    void requirePositive_rejectsZeroAndNegative() {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> Validation.requirePositive(0, "duration"));
        assertThrows(IllegalArgumentException.class, () -> Validation.requirePositive(-5, "duration"));

        assertEquals("duration must be positive", error.getMessage());
        assertDoesNotThrow(() -> Validation.requirePositive(1, "duration"));
    }

    @Test
    void requireUser_checksNamesAndPresence() {
        assertThrows(IllegalArgumentException.class, () -> Validation.requireUser(null));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireUser(User.of("", "Smith")));
        assertThrows(IllegalArgumentException.class, () -> Validation.requireUser(User.of("John", null)));
        assertDoesNotThrow(() -> Validation.requireUser(User.of("John", "Smith")));
    }
}
