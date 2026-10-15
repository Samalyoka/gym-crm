package com.epam.gymcrm.infrastructure.facade;

import com.epam.gymcrm.application.service.AuthenticationService;
import com.epam.gymcrm.application.service.NewTraining;
import com.epam.gymcrm.application.service.TraineeService;
import com.epam.gymcrm.application.service.TrainerService;
import com.epam.gymcrm.application.service.TrainingService;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.Training;
import com.epam.gymcrm.domain.model.TrainingType;
import com.epam.gymcrm.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The facade only delegates, so every test checks that the right service is called.
 */
@ExtendWith(MockitoExtension.class)
class GymFacadeTest {

    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 12, 31);

    @Mock
    private TraineeService traineeService;
    @Mock
    private TrainerService trainerService;
    @Mock
    private TrainingService trainingService;
    @Mock
    private AuthenticationService authenticationService;
    @InjectMocks
    private GymFacade facade;

    private final Trainee trainee = new Trainee(1L, new User("John", "Smith", "John.Smith", "p", true), null, null);
    private final Trainer trainer = new Trainer(5L, new User("Anna", "Brown", "Anna.Brown", "p", true),
            new TrainingType("YOGA"));
    private final Training training = new Training(1L, 1L, 5L, "Yoga", new TrainingType("YOGA"), FROM, 60);

    @Test
    void createTrainer_delegates() {
        when(trainerService.create(trainer)).thenReturn(trainer);

        assertSame(trainer, facade.createTrainer(trainer));
    }

    @Test
    void createTrainee_delegates() {
        when(traineeService.create(trainee)).thenReturn(trainee);

        assertSame(trainee, facade.createTrainee(trainee));
    }

    @Test
    void matchesTrainee_delegates() {
        when(authenticationService.matchesTrainee("u", "p")).thenReturn(true);

        assertTrue(facade.matchesTrainee("u", "p"));
    }

    @Test
    void matchesTrainer_delegates() {
        when(authenticationService.matchesTrainer("u", "p")).thenReturn(false);

        assertFalse(facade.matchesTrainer("u", "p"));
    }

    @Test
    void getTrainer_delegates() {
        when(trainerService.getByUsername("u", "p")).thenReturn(trainer);

        assertSame(trainer, facade.getTrainer("u", "p"));
    }

    @Test
    void getTrainee_delegates() {
        when(traineeService.getByUsername("u", "p")).thenReturn(trainee);

        assertSame(trainee, facade.getTrainee("u", "p"));
    }

    @Test
    void changeTraineePassword_delegates() {
        facade.changeTraineePassword("u", "old", "new");

        verify(traineeService).changePassword("u", "old", "new");
    }

    @Test
    void changeTrainerPassword_delegates() {
        facade.changeTrainerPassword("u", "old", "new");

        verify(trainerService).changePassword("u", "old", "new");
    }

    @Test
    void updateTrainer_delegates() {
        when(trainerService.update("u", "p", trainer)).thenReturn(trainer);

        assertSame(trainer, facade.updateTrainer("u", "p", trainer));
    }

    @Test
    void updateTrainee_delegates() {
        when(traineeService.update("u", "p", trainee)).thenReturn(trainee);

        assertSame(trainee, facade.updateTrainee("u", "p", trainee));
    }

    @Test
    void toggleTraineeActive_delegates() {
        when(traineeService.toggleActive("u", "p")).thenReturn(false);

        assertFalse(facade.toggleTraineeActive("u", "p"));
    }

    @Test
    void toggleTrainerActive_delegates() {
        when(trainerService.toggleActive("u", "p")).thenReturn(true);

        assertTrue(facade.toggleTrainerActive("u", "p"));
    }

    @Test
    void deleteTrainee_delegates() {
        facade.deleteTrainee("u", "p");

        verify(traineeService).delete("u", "p");
    }

    @Test
    void getTraineeTrainings_delegates() {
        when(trainingService.getTraineeTrainings("u", "p", FROM, TO, "Anna", "YOGA"))
                .thenReturn(List.of(training));

        assertSame(training, facade.getTraineeTrainings("u", "p", FROM, TO, "Anna", "YOGA").get(0));
    }

    @Test
    void getTrainerTrainings_delegates() {
        when(trainingService.getTrainerTrainings("u", "p", FROM, TO, "John")).thenReturn(List.of(training));

        assertSame(training, facade.getTrainerTrainings("u", "p", FROM, TO, "John").get(0));
    }

    @Test
    void addTraining_delegates() {
        NewTraining request = new NewTraining("John.Smith", "Yoga", new TrainingType("YOGA"), FROM, 60);
        when(trainingService.addTraining("u", "p", request)).thenReturn(training);

        assertSame(training, facade.addTraining("u", "p", request));
    }

    @Test
    void getNotAssignedTrainers_delegates() {
        when(traineeService.getNotAssignedTrainers("u", "p")).thenReturn(List.of(trainer));

        assertSame(trainer, facade.getNotAssignedTrainers("u", "p").get(0));
    }

    @Test
    void updateTraineeTrainers_delegates() {
        when(traineeService.updateTrainers("u", "p", List.of("Anna.Brown"))).thenReturn(List.of(trainer));

        assertSame(trainer, facade.updateTraineeTrainers("u", "p", List.of("Anna.Brown")).get(0));
    }
}
