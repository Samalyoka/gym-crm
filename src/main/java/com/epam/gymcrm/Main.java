package com.epam.gymcrm;

import com.epam.gymcrm.application.service.NewTraining;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.TrainingType;
import com.epam.gymcrm.domain.model.User;
import com.epam.gymcrm.infrastructure.config.AppConfig;
import com.epam.gymcrm.infrastructure.facade.GymFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.LocalDate;
import java.util.List;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try (var context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            GymFacade facade = context.getBean(GymFacade.class);

            Trainer trainer = facade.createTrainer(
                    new Trainer(null, User.of("Anna", "Brown"), new TrainingType("YOGA")));
            Trainee trainee = facade.createTrainee(
                    new Trainee(null, User.of("John", "Smith"), LocalDate.of(2000, 1, 1), "Astana"));
            log.info("Created: {} and {}", trainer, trainee);

            String trainerLogin = trainer.user().username();
            String trainerPassword = trainer.user().password();
            String traineeLogin = trainee.user().username();
            String traineePassword = trainee.user().password();

            log.info("Credentials match: {}", facade.matchesTrainee(traineeLogin, traineePassword));
            log.info("Wrong password matches: {}", facade.matchesTrainee(traineeLogin, "wrong"));

            log.info("Not assigned trainers: {}",
                    facade.getNotAssignedTrainers(traineeLogin, traineePassword).size());
            facade.updateTraineeTrainers(traineeLogin, traineePassword, List.of(trainerLogin));
            log.info("Not assigned trainers after assigning: {}",
                    facade.getNotAssignedTrainers(traineeLogin, traineePassword).size());

            facade.addTraining(trainerLogin, trainerPassword,
                    new NewTraining(traineeLogin, "Morning yoga", new TrainingType("YOGA"),
                            LocalDate.now(), 60));
            log.info("Trainee trainings: {}",
                    facade.getTraineeTrainings(traineeLogin, traineePassword, null, null, null, "YOGA"));
            log.info("Trainer trainings: {}",
                    facade.getTrainerTrainings(trainerLogin, trainerPassword, null, null, "John"));

            log.info("Trainee active after toggle: {}", facade.toggleTraineeActive(traineeLogin, traineePassword));
            facade.changeTraineePassword(traineeLogin, traineePassword, "NewPass123");

            facade.deleteTrainee(traineeLogin, "NewPass123");
            log.info("Trainee still can log in after delete: {}", facade.matchesTrainee(traineeLogin, "NewPass123"));
            log.info("Trainer trainings after trainee delete: {}",
                    facade.getTrainerTrainings(trainerLogin, trainerPassword, null, null, null).size());
        }
    }
}
