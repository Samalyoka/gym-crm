package com.epam.gymcrm.application.service;

import com.epam.gymcrm.domain.model.TrainingType;

import java.time.LocalDate;

public record NewTraining(String traineeUsername, String name, TrainingType type,
                          LocalDate date, int duration) {
}
