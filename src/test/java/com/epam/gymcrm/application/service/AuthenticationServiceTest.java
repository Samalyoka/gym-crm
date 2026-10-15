package com.epam.gymcrm.application.service;

import com.epam.gymcrm.application.port.out.TraineeDao;
import com.epam.gymcrm.application.port.out.TrainerDao;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.TrainingType;
import com.epam.gymcrm.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
    @InjectMocks
    private AuthenticationService service;

    private Trainee traineeWithPassword(String password) {
        User user = new User("John", "Smith", "John.Smith", password, true);
        return new Trainee(1L, user, LocalDate.of(2000, 1, 1), "Astana");
    }

    private Trainer trainerWithPassword(String password) {
        User user = new User("Anna", "Brown", "Anna.Brown", password, true);
        return new Trainer(5L, user, new TrainingType("YOGA"));
    }

    @Test
    void matchesTrainee_returnsTrue_forCorrectPassword() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(traineeWithPassword("secret")));

        assertTrue(service.matchesTrainee("John.Smith", "secret"));
    }

    @Test
    void matchesTrainee_returnsFalse_forWrongPassword() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(traineeWithPassword("secret")));

        assertFalse(service.matchesTrainee("John.Smith", "wrong"));
    }

    @Test
    void matchesTrainee_returnsFalse_whenPasswordIsNull() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(traineeWithPassword("secret")));

        assertFalse(service.matchesTrainee("John.Smith", null));
    }

    @Test
    void matchesTrainee_returnsFalse_whenUserDoesNotExist() {
        when(traineeDao.findByUsername("nobody")).thenReturn(Optional.empty());

        assertFalse(service.matchesTrainee("nobody", "secret"));
    }

    @Test
    void authenticateTrainee_throws_forWrongPassword() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(traineeWithPassword("secret")));

        assertThrows(AuthenticationException.class, () -> service.authenticateTrainee("John.Smith", "wrong"));
    }

    @Test
    void authenticateTrainee_passes_forCorrectPassword() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(traineeWithPassword("secret")));

        assertDoesNotThrow(() -> service.authenticateTrainee("John.Smith", "secret"));
    }

    @Test
    void matchesTrainer_returnsTrue_forCorrectPassword() {
        when(trainerDao.findByUsername("Anna.Brown")).thenReturn(Optional.of(trainerWithPassword("secret")));

        assertTrue(service.matchesTrainer("Anna.Brown", "secret"));
    }

    @Test
    void matchesTrainer_returnsFalse_forWrongPasswordOrUnknownUser() {
        when(trainerDao.findByUsername("Anna.Brown")).thenReturn(Optional.of(trainerWithPassword("secret")));
        when(trainerDao.findByUsername("nobody")).thenReturn(Optional.empty());

        assertFalse(service.matchesTrainer("Anna.Brown", "wrong"));
        assertFalse(service.matchesTrainer("nobody", "secret"));
    }

    @Test
    void authenticateTrainer_throws_forWrongPassword() {
        when(trainerDao.findByUsername("Anna.Brown")).thenReturn(Optional.of(trainerWithPassword("secret")));

        assertThrows(AuthenticationException.class, () -> service.authenticateTrainer("Anna.Brown", "wrong"));
    }

    @Test
    void authenticateTrainer_passes_forCorrectPassword() {
        when(trainerDao.findByUsername("Anna.Brown")).thenReturn(Optional.of(trainerWithPassword("secret")));

        assertDoesNotThrow(() -> service.authenticateTrainer("Anna.Brown", "secret"));
    }
}
