package com.epam.gymcrm.infrastructure.persistence;

import com.epam.gymcrm.application.port.out.TrainingDao;
import com.epam.gymcrm.domain.model.Training;
import com.epam.gymcrm.infrastructure.persistence.entity.TraineeEntity;
import com.epam.gymcrm.infrastructure.persistence.entity.TrainerEntity;
import com.epam.gymcrm.infrastructure.persistence.entity.TrainingEntity;
import com.epam.gymcrm.infrastructure.persistence.entity.TrainingTypeEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
public class TrainingDaoImpl extends AbstractJpaDao implements TrainingDao {

    private static final Logger log = LoggerFactory.getLogger(TrainingDaoImpl.class);

    @Override
    public Training save(Training training) {
        if (training.id() != null) {
            throw new IllegalArgumentException("Updating a training is not supported");
        }
        TraineeEntity trainee = entityManager.find(TraineeEntity.class, training.traineeId());
        if (trainee == null) {
            throw new NoSuchElementException("Trainee not found: " + training.traineeId());
        }
        TrainerEntity trainer = entityManager.find(TrainerEntity.class, training.trainerId());
        if (trainer == null) {
            throw new NoSuchElementException("Trainer not found: " + training.trainerId());
        }
        TrainingTypeEntity type = findTrainingType(training.trainingType().name());

        TrainingEntity entity = EntityMapper.toTrainingEntity(training, trainee, trainer, type);
        entityManager.persist(entity);
        log.debug("Persisted training id={}", entity.getId());
        return EntityMapper.toTraining(entity);
    }

    @Override
    public Optional<Training> findById(Long id) {
        return Optional.ofNullable(entityManager.find(TrainingEntity.class, id))
                .map(EntityMapper::toTraining);
    }

    @Override
    public List<Training> findAll() {
        return entityManager.createQuery(
                        "select t from TrainingEntity t join fetch t.trainingType", TrainingEntity.class)
                .getResultList().stream()
                .map(EntityMapper::toTraining)
                .toList();
    }

    @Override
    public List<Training> findForTrainee(String traineeUsername, LocalDate from, LocalDate to,
                                         String trainerName, String trainingTypeName) {
        return search("trainee", traineeUsername, "trainer", trainerName, from, to, trainingTypeName);
    }

    @Override
    public List<Training> findForTrainer(String trainerUsername, LocalDate from, LocalDate to,
                                         String traineeName) {
        return search("trainer", trainerUsername, "trainee", traineeName, from, to, null);
    }

    /**
     * Builds the query dynamically: only the filters that were actually passed are added.
     * ownerAttribute is the side whose username we search by, otherAttribute is the side filtered by name.
     */
    private List<Training> search(String ownerAttribute, String ownerUsername,
                                  String otherAttribute, String otherFirstName,
                                  LocalDate from, LocalDate to, String trainingTypeName) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TrainingEntity> query = cb.createQuery(TrainingEntity.class);
        Root<TrainingEntity> training = query.from(TrainingEntity.class);
        training.fetch("trainingType");

        List<Predicate> filters = new ArrayList<>();
        filters.add(cb.equal(training.get(ownerAttribute).get("user").get("username"), ownerUsername));
        if (from != null) {
            filters.add(cb.greaterThanOrEqualTo(training.<LocalDate>get("date"), from));
        }
        if (to != null) {
            filters.add(cb.lessThanOrEqualTo(training.<LocalDate>get("date"), to));
        }
        if (hasText(otherFirstName)) {
            filters.add(cb.equal(
                    cb.lower(training.get(otherAttribute).get("user").<String>get("firstName")),
                    otherFirstName.toLowerCase()));
        }
        if (hasText(trainingTypeName)) {
            filters.add(cb.equal(training.get("trainingType").get("name"), trainingTypeName));
        }
        query.select(training)
                .where(filters.toArray(new Predicate[0]))
                .orderBy(cb.asc(training.get("date")));

        return entityManager.createQuery(query).getResultList().stream()
                .map(EntityMapper::toTraining)
                .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
