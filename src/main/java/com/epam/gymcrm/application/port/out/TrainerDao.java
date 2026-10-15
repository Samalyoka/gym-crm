package com.epam.gymcrm.application.port.out;

import com.epam.gymcrm.domain.model.Trainer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainerDao extends BaseDao<Trainer>, UsernameLookup {

    Optional<Trainer> findByUsername(String username);

    List<Trainer> findByUsernames(Collection<String> usernames);

    List<Trainer> findNotAssignedTo(String traineeUsername);
}
