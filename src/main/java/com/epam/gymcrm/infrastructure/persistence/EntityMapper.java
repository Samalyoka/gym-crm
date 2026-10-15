package com.epam.gymcrm.infrastructure.persistence;

import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.Training;
import com.epam.gymcrm.domain.model.TrainingType;
import com.epam.gymcrm.domain.model.User;
import com.epam.gymcrm.infrastructure.persistence.entity.TraineeEntity;
import com.epam.gymcrm.infrastructure.persistence.entity.TrainerEntity;
import com.epam.gymcrm.infrastructure.persistence.entity.TrainingEntity;
import com.epam.gymcrm.infrastructure.persistence.entity.TrainingTypeEntity;
import com.epam.gymcrm.infrastructure.persistence.entity.UserEntity;

/**
 * Converts between domain records and JPA entities.
 * Keeps the domain model free of persistence annotations.
 */
public final class EntityMapper {

    private EntityMapper() {
    }

    // ---------- entity -> domain ----------

    public static User toUser(UserEntity entity) {
        return new User(entity.getFirstName(), entity.getLastName(), entity.getUsername(),
                entity.getPassword(), entity.isActive());
    }

    public static TrainingType toTrainingType(TrainingTypeEntity entity) {
        return new TrainingType(entity.getName());
    }

    public static Trainee toTrainee(TraineeEntity entity) {
        return new Trainee(entity.getId(), toUser(entity.getUser()),
                entity.getDateOfBirth(), entity.getAddress());
    }

    public static Trainer toTrainer(TrainerEntity entity) {
        return new Trainer(entity.getId(), toUser(entity.getUser()),
                toTrainingType(entity.getSpecialization()));
    }

    public static Training toTraining(TrainingEntity entity) {
        return new Training(entity.getId(), entity.getTrainee().getId(), entity.getTrainer().getId(),
                entity.getName(), toTrainingType(entity.getTrainingType()),
                entity.getDate(), entity.getDuration());
    }

    // ---------- domain -> entity (new rows) ----------

    public static UserEntity toUserEntity(User user) {
        UserEntity entity = new UserEntity();
        applyUser(entity, user);
        return entity;
    }

    public static TraineeEntity toTraineeEntity(Trainee trainee) {
        TraineeEntity entity = new TraineeEntity();
        entity.setUser(toUserEntity(trainee.user()));
        entity.setDateOfBirth(trainee.dateOfBirth());
        entity.setAddress(trainee.address());
        return entity;
    }

    public static TrainerEntity toTrainerEntity(Trainer trainer, TrainingTypeEntity specialization) {
        TrainerEntity entity = new TrainerEntity();
        entity.setUser(toUserEntity(trainer.user()));
        entity.setSpecialization(specialization);
        return entity;
    }

    public static TrainingEntity toTrainingEntity(Training training, TraineeEntity trainee,
                                                  TrainerEntity trainer, TrainingTypeEntity type) {
        TrainingEntity entity = new TrainingEntity();
        entity.setTrainee(trainee);
        entity.setTrainer(trainer);
        entity.setTrainingType(type);
        entity.setName(training.trainingName());
        entity.setDate(training.trainingDate());
        entity.setDuration(training.trainingDuration());
        return entity;
    }

    // ---------- domain -> existing entity (updates) ----------

    public static void applyUser(UserEntity target, User source) {
        target.setFirstName(source.firstName());
        target.setLastName(source.lastName());
        target.setUsername(source.username());
        target.setPassword(source.password());
        target.setActive(source.isActive());
    }

    public static void applyTrainee(TraineeEntity target, Trainee source) {
        applyUser(target.getUser(), source.user());
        target.setDateOfBirth(source.dateOfBirth());
        target.setAddress(source.address());
    }

    public static void applyTrainer(TrainerEntity target, Trainer source,
                                    TrainingTypeEntity specialization) {
        applyUser(target.getUser(), source.user());
        target.setSpecialization(specialization);
    }
}
