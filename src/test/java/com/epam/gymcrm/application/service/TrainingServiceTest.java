package com.epam.gymcrm.application.service;

import com.epam.gymcrm.application.port.out.TraineeDao;
import com.epam.gymcrm.application.port.out.TrainerDao;
import com.epam.gymcrm.application.port.out.TrainingDao;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.Training;
import com.epam.gymcrm.domain.model.TrainingType;
import com.epam.gymcrm.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 1, 10);

    @Mock
    private TrainingDao trainingDao;
    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
    @Mock
    private AuthenticationService authenticationService;
    @InjectMocks
    private TrainingService service;

    private Trainer trainer() {
        return new Trainer(5L, new User("Anna", "Brown", "Anna.Brown", "secret", true), new TrainingType("YOGA"));
    }

    private Trainee trainee() {
        return new Trainee(1L, new User("John", "Smith", "John.Smith", "secret", true), null, null);
    }

    private NewTraining request(String name, int duration) {
        return new NewTraining("John.Smith", name, new TrainingType("YOGA"), DATE, duration);
    }

    // ---------- add training ----------

    @Test
    void addTraining_savesTrainingWithIdsOfTraineeAndTrainer() {
        when(trainerDao.findByUsername("Anna.Brown")).thenReturn(Optional.of(trainer()));
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(trainee()));
        when(trainingDao.save(any(Training.class))).thenAnswer(invocation -> {
            Training toSave = invocation.getArgument(0);
            return toSave.withId(10L);
        });

        Training result = service.addTraining("Anna.Brown", "secret", request("Yoga", 60));

        assertEquals(10L, result.id());
        ArgumentCaptor<Training> captor = ArgumentCaptor.forClass(Training.class);
        verify(trainingDao).save(captor.capture());
        Training saved = captor.getValue();
        assertEquals(1L, saved.traineeId());
        assertEquals(5L, saved.trainerId());
        assertEquals("Yoga", saved.trainingName());
        assertEquals("YOGA", saved.trainingType().name());
        assertEquals(DATE, saved.trainingDate());
        assertEquals(60, saved.trainingDuration());
        verify(authenticationService).authenticateTrainer("Anna.Brown", "secret");
    }

    @Test
    void addTraining_withWrongPassword_throwsAndSavesNothing() {
        doThrow(new AuthenticationException()).when(authenticationService)
                .authenticateTrainer("Anna.Brown", "bad");

        assertThrows(AuthenticationException.class,
                () -> service.addTraining("Anna.Brown", "bad", request("Yoga", 60)));
        verifyNoInteractions(trainingDao);
    }

    @Test
    void addTraining_withUnknownTrainee_throwsAndSavesNothing() {
        when(trainerDao.findByUsername("Anna.Brown")).thenReturn(Optional.of(trainer()));
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.addTraining("Anna.Brown", "secret", request("Yoga", 60)));
        verify(trainingDao, never()).save(any());
    }

    @Test
    void addTraining_withUnknownTrainer_throwsAndSavesNothing() {
        when(trainerDao.findByUsername("Anna.Brown")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.addTraining("Anna.Brown", "secret", request("Yoga", 60)));
        verify(trainingDao, never()).save(any());
    }

    @Test
    void addTraining_withBlankName_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addTraining("Anna.Brown", "secret", request(" ", 60)));
        verifyNoInteractions(trainingDao);
    }

    @Test
    void addTraining_withNonPositiveDuration_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addTraining("Anna.Brown", "secret", request("Yoga", 0)));
        verifyNoInteractions(trainingDao);
    }

    @Test
    void addTraining_withMissingDate_throws() {
        NewTraining noDate = new NewTraining("John.Smith", "Yoga", new TrainingType("YOGA"), null, 60);

        assertThrows(IllegalArgumentException.class, () -> service.addTraining("Anna.Brown", "secret", noDate));
    }

    @Test
    void addTraining_withMissingType_throws() {
        NewTraining noType = new NewTraining("John.Smith", "Yoga", null, DATE, 60);

        assertThrows(IllegalArgumentException.class, () -> service.addTraining("Anna.Brown", "secret", noType));
    }

    @Test
    void addTraining_withMissingTraineeUsername_throws() {
        NewTraining noTrainee = new NewTraining(" ", "Yoga", new TrainingType("YOGA"), DATE, 60);

        assertThrows(IllegalArgumentException.class, () -> service.addTraining("Anna.Brown", "secret", noTrainee));
    }

    @Test
    void addTraining_withNullRequest_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.addTraining("Anna.Brown", "secret", null));
    }

    // ---------- lists ----------

    @Test
    void getTraineeTrainings_passesCriteriaToDao() {
        LocalDate to = LocalDate.of(2026, 12, 31);
        List<Training> trainings = List.of(new Training(1L, 1L, 5L, "Yoga", new TrainingType("YOGA"), DATE, 60));
        when(trainingDao.findForTrainee("John.Smith", DATE, to, "Anna", "YOGA")).thenReturn(trainings);

        List<Training> result = service.getTraineeTrainings("John.Smith", "secret", DATE, to, "Anna", "YOGA");

        assertSame(trainings, result);
        verify(authenticationService).authenticateTrainee("John.Smith", "secret");
    }

    @Test
    void getTraineeTrainings_withWrongPassword_throwsAndDoesNotTouchDao() {
        doThrow(new AuthenticationException()).when(authenticationService)
                .authenticateTrainee("John.Smith", "bad");

        assertThrows(AuthenticationException.class,
                () -> service.getTraineeTrainings("John.Smith", "bad", null, null, null, null));
        verifyNoInteractions(trainingDao);
    }

    @Test
    void getTrainerTrainings_passesCriteriaToDao() {
        List<Training> trainings = List.of(new Training(1L, 1L, 5L, "Yoga", new TrainingType("YOGA"), DATE, 60));
        when(trainingDao.findForTrainer("Anna.Brown", DATE, null, "John")).thenReturn(trainings);

        List<Training> result = service.getTrainerTrainings("Anna.Brown", "secret", DATE, null, "John");

        assertSame(trainings, result);
        verify(authenticationService).authenticateTrainer("Anna.Brown", "secret");
    }

    @Test
    void getTrainerTrainings_withWrongPassword_throwsAndDoesNotTouchDao() {
        doThrow(new AuthenticationException()).when(authenticationService)
                .authenticateTrainer("Anna.Brown", "bad");

        assertThrows(AuthenticationException.class,
                () -> service.getTrainerTrainings("Anna.Brown", "bad", null, null, null));
        verifyNoInteractions(trainingDao);
    }
}
