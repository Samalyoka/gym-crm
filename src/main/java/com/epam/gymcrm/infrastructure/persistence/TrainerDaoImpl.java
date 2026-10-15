package com.epam.gymcrm.infrastructure.persistence;

import com.epam.gymcrm.application.port.out.TrainerDao;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.infrastructure.persistence.entity.TrainerEntity;
import com.epam.gymcrm.infrastructure.persistence.entity.TrainingTypeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
public class TrainerDaoImpl extends AbstractJpaDao implements TrainerDao {

    private static final Logger log = LoggerFactory.getLogger(TrainerDaoImpl.class);

    private static final String SELECT_TRAINER =
            "select t from TrainerEntity t join fetch t.user u join fetch t.specialization ";

    @Override
    public Trainer save(Trainer trainer) {
        TrainingTypeEntity specialization = findTrainingType(trainer.specialization().name());
        if (trainer.id() == null) {
            TrainerEntity entity = EntityMapper.toTrainerEntity(trainer, specialization);
            entityManager.persist(entity);
            log.debug("Persisted trainer id={}", entity.getId());
            return EntityMapper.toTrainer(entity);
        }
        TrainerEntity entity = entityManager.find(TrainerEntity.class, trainer.id());
        if (entity == null) {
            throw new NoSuchElementException("Trainer not found: " + trainer.id());
        }
        EntityMapper.applyTrainer(entity, trainer, specialization);
        log.debug("Updated trainer id={}", entity.getId());
        return EntityMapper.toTrainer(entity);
    }

    @Override
    public Optional<Trainer> findById(Long id) {
        return Optional.ofNullable(entityManager.find(TrainerEntity.class, id))
                .map(EntityMapper::toTrainer);
    }

    @Override
    public Optional<Trainer> findByUsername(String username) {
        return entityManager.createQuery(SELECT_TRAINER + "where u.username = :username", TrainerEntity.class)
                .setParameter("username", username)
                .getResultList().stream().findFirst()
                .map(EntityMapper::toTrainer);
    }

    @Override
    public List<Trainer> findByUsernames(Collection<String> usernames) {
        if (usernames.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery(SELECT_TRAINER + "where u.username in :usernames", TrainerEntity.class)
                .setParameter("usernames", usernames)
                .getResultList().stream()
                .map(EntityMapper::toTrainer)
                .toList();
    }

    @Override
    public List<Trainer> findAll() {
        return entityManager.createQuery(SELECT_TRAINER, TrainerEntity.class)
                .getResultList().stream()
                .map(EntityMapper::toTrainer)
                .toList();
    }

    @Override
    public List<Trainer> findNotAssignedTo(String traineeUsername) {
        return entityManager.createQuery(SELECT_TRAINER
                                + "where t.id not in (select a.id from TraineeEntity te "
                                + "join te.trainers a join te.user tu where tu.username = :username)",
                        TrainerEntity.class)
                .setParameter("username", traineeUsername)
                .getResultList().stream()
                .map(EntityMapper::toTrainer)
                .toList();
    }
}
