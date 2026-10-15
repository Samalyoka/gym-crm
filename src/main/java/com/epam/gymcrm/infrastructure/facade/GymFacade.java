package com.epam.gymcrm.infrastructure.facade;

import com.epam.gymcrm.application.service.AuthenticationService;
import com.epam.gymcrm.application.service.NewTraining;
import com.epam.gymcrm.application.service.TraineeService;
import com.epam.gymcrm.application.service.TrainerService;
import com.epam.gymcrm.application.service.TrainingService;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.Training;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Single entry point for all operations of the application; only delegates to the services.
 */
@Component
public class GymFacade {

    private final TraineeService traineeService;
    private final TrainerService trainerService;
    private final TrainingService trainingService;
    private final AuthenticationService authenticationService;

    public GymFacade(TraineeService traineeService, TrainerService trainerService,
                     TrainingService trainingService, AuthenticationService authenticationService) {
        this.traineeService = traineeService;
        this.trainerService = trainerService;
        this.trainingService = trainingService;
        this.authenticationService = authenticationService;
    }

    public Trainer createTrainer(Trainer trainer) {
        return trainerService.create(trainer);
    }

    public Trainee createTrainee(Trainee trainee) {
        return traineeService.create(trainee);
    }

    public boolean matchesTrainee(String username, String password) {
        return authenticationService.matchesTrainee(username, password);
    }

    public boolean matchesTrainer(String username, String password) {
        return authenticationService.matchesTrainer(username, password);
    }

    public Trainer getTrainer(String username, String password) {
        return trainerService.getByUsername(username, password);
    }

    public Trainee getTrainee(String username, String password) {
        return traineeService.getByUsername(username, password);
    }

    public void changeTraineePassword(String username, String oldPassword, String newPassword) {
        traineeService.changePassword(username, oldPassword, newPassword);
    }

    public void changeTrainerPassword(String username, String oldPassword, String newPassword) {
        trainerService.changePassword(username, oldPassword, newPassword);
    }

    public Trainer updateTrainer(String username, String password, Trainer changes) {
        return trainerService.update(username, password, changes);
    }

    public Trainee updateTrainee(String username, String password, Trainee changes) {
        return traineeService.update(username, password, changes);
    }

    public boolean toggleTraineeActive(String username, String password) {
        return traineeService.toggleActive(username, password);
    }

    public boolean toggleTrainerActive(String username, String password) {
        return trainerService.toggleActive(username, password);
    }

    public void deleteTrainee(String username, String password) {
        traineeService.delete(username, password);
    }

    public List<Training> getTraineeTrainings(String username, String password, LocalDate from,
                                              LocalDate to, String trainerName, String trainingTypeName) {
        return trainingService.getTraineeTrainings(username, password, from, to, trainerName, trainingTypeName);
    }

    public List<Training> getTrainerTrainings(String username, String password, LocalDate from,
                                              LocalDate to, String traineeName) {
        return trainingService.getTrainerTrainings(username, password, from, to, traineeName);
    }

    public Training addTraining(String trainerUsername, String trainerPassword, NewTraining training) {
        return trainingService.addTraining(trainerUsername, trainerPassword, training);
    }

    public List<Trainer> getNotAssignedTrainers(String traineeUsername, String traineePassword) {
        return traineeService.getNotAssignedTrainers(traineeUsername, traineePassword);
    }

    public List<Trainer> updateTraineeTrainers(String username, String password, List<String> trainerUsernames) {
        return traineeService.updateTrainers(username, password, trainerUsernames);
    }
}
