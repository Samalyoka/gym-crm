package com.epam.gymcrm.application.port.out;

import com.epam.gymcrm.domain.model.Training;

import java.time.LocalDate;
import java.util.List;

public interface TrainingDao extends BaseDao<Training> {

    /** Any filter except the username may be null. */
    List<Training> findForTrainee(String traineeUsername, LocalDate from, LocalDate to,
                                  String trainerName, String trainingTypeName);

    /** Any filter except the username may be null. */
    List<Training> findForTrainer(String trainerUsername, LocalDate from, LocalDate to,
                                  String traineeName);
}
