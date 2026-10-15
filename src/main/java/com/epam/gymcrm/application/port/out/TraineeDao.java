package com.epam.gymcrm.application.port.out;

import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TraineeDao extends BaseDao<Trainee>, UsernameLookup {

    Optional<Trainee> findByUsername(String username);

    void deleteById(Long id);

    /** Replaces the trainee's trainers with the given ones and returns the new list. */
    List<Trainer> updateTrainers(Long traineeId, Set<Long> trainerIds);
}
