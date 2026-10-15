package com.epam.gymcrm.support;

import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.TrainingType;
import com.epam.gymcrm.domain.model.User;

import java.time.LocalDate;

public final class TestData {

    private TestData() {
    }

    public static Trainee trainee(String first, String last) {
        return new Trainee(null, user(first, last), LocalDate.of(2000, 1, 1), "Astana");
    }

    public static Trainer trainer(String first, String last, String specialization) {
        return new Trainer(null, user(first, last), new TrainingType(specialization));
    }

    private static User user(String first, String last) {
        return User.of(first, last).withCredentials(first + "." + last, "pass");
    }
}
