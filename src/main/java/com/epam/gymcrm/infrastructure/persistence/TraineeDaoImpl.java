package com.epam.gymcrm.infrastructure.persistence;

import com.epam.gymcrm.application.port.out.TraineeDao;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.infrastructure.persistence.entity.TraineeEntity;
import com.epam.gymcrm.infrastructure.persistence.entity.TrainerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Repository
public class TraineeDaoImpl extends AbstractJpaDao implements TraineeDao {

    private static final Logger log = LoggerFactory.getLogger(TraineeDaoImpl.class);

    private static final String SELECT_TRAINEE = "select t from TraineeEntity t join fetch t.user u ";

    @Override
    public Trainee save(Trainee trainee) {
        if (trainee.id() == null) {
            TraineeEntity entity = EntityMapper.toTraineeEntity(trainee);
            entityManager.persist(entity);
            log.debug("Persisted trainee id={}", entity.getId());
            return EntityMapper.toTrainee(entity);
        }
        TraineeEntity entity = getEntity(trainee.id());
        EntityMapper.applyTrainee(entity, trainee);
        log.debug("Updated trainee id={}", entity.getId());
        return EntityMapper.toTrainee(entity);
    }

    @Override
    public Optional<Trainee> findById(Long id) {
        return Optional.ofNullable(entityManager.find(TraineeEntity.class, id))
                .map(EntityMapper::toTrainee);
    }

    @Override
    public Optional<Trainee> findByUsername(String username) {
        return entityManager.createQuery(SELECT_TRAINEE + "where u.username = :username", TraineeEntity.class)
                .setParameter("username", username)
                .getResultList().stream().findFirst()
                .map(EntityMapper::toTrainee);
    }

    @Override
    public List<Trainee> findAll() {
        return entityManager.createQuery(SELECT_TRAINEE, TraineeEntity.class)
                .getResultList().stream()
                .map(EntityMapper::toTrainee)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        TraineeEntity entity = entityManager.find(TraineeEntity.class, id);
        if (entity != null) {
            entityManager.remove(entity);
            log.debug("Removed trainee id={}", id);
        }
    }

    @Override
    public List<Trainer> updateTrainers(Long traineeId, Set<Long> trainerIds) {
        TraineeEntity trainee = getEntity(traineeId);
        Set<TrainerEntity> trainers = new HashSet<>();
        if (!trainerIds.isEmpty()) {
            trainers.addAll(entityManager.createQuery(
                            "select t from TrainerEntity t join fetch t.user join fetch t.specialization "
                                    + "where t.id in :ids", TrainerEntity.class)
                    .setParameter("ids", trainerIds)
                    .getResultList());
        }
        if (trainers.size() != trainerIds.size()) {
            throw new NoSuchElementException("Some trainers were not found: " + trainerIds);
        }
        trainee.getTrainers().clear();
        trainee.getTrainers().addAll(trainers);
        log.debug("Trainee id={} now has {} trainers", traineeId, trainers.size());
        return trainers.stream().map(EntityMapper::toTrainer).toList();
    }

    private TraineeEntity getEntity(Long id) {
        TraineeEntity entity = entityManager.find(TraineeEntity.class, id);
        if (entity == null) {
            throw new NoSuchElementException("Trainee not found: " + id);
        }
        return entity;
    }
}
