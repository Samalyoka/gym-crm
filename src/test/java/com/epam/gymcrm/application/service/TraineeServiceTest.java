package com.epam.gymcrm.application.service;

import com.epam.gymcrm.application.port.out.TraineeDao;
import com.epam.gymcrm.application.port.out.TrainerDao;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraineeServiceTest {

    @Mock
    private TraineeDao traineeDao;
    @Mock
    private TrainerDao trainerDao;
    @Mock
    private UsernameGenerator usernameGenerator;
    @Mock
    private PasswordGenerator passwordGenerator;
    @Mock
    private AuthenticationService authenticationService;
    @InjectMocks
    private TraineeService service;

    private Trainee existingTrainee(boolean active) {
        User user = new User("John", "Smith", "John.Smith", "secret", active);
        return new Trainee(1L, user, LocalDate.of(2000, 1, 1), "Astana");
    }

    private void givenExistingTrainee(boolean active) {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.of(existingTrainee(active)));
    }

    private Trainee savedTrainee() {
        ArgumentCaptor<Trainee> captor = ArgumentCaptor.forClass(Trainee.class);
        verify(traineeDao).save(captor.capture());
        return captor.getValue();
    }

    // ---------- create ----------

    @Test
    void create_generatesCredentialsAndSaves() {
        when(usernameGenerator.generate("John", "Smith")).thenReturn("John.Smith");
        when(passwordGenerator.generate()).thenReturn("pass123456");
        when(traineeDao.save(any(Trainee.class))).thenAnswer(invocation -> {
            Trainee toSave = invocation.getArgument(0);
            return toSave.withId(1L);
        });

        Trainee result = service.create(new Trainee(null, User.of("John", "Smith"), null, null));

        assertEquals(1L, result.id());
        assertEquals("John.Smith", result.user().username());
        assertEquals("pass123456", result.user().password());
        assertTrue(result.user().isActive());
    }

    @Test
    void create_withBlankFirstName_throwsAndSavesNothing() {
        Trainee invalid = new Trainee(null, User.of(" ", "Smith"), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.create(invalid));
        verifyNoInteractions(traineeDao);
    }

    @Test
    void create_withNullTrainee_throws() {
        assertThrows(IllegalArgumentException.class, () -> service.create(null));
    }

    // ---------- get / authentication ----------

    @Test
    void getByUsername_returnsTrainee() {
        givenExistingTrainee(true);

        Trainee result = service.getByUsername("John.Smith", "secret");

        assertEquals(1L, result.id());
        verify(authenticationService).authenticateTrainee("John.Smith", "secret");
    }

    @Test
    void getByUsername_withWrongPassword_throwsAndDoesNotTouchDao() {
        doThrow(new AuthenticationException()).when(authenticationService)
                .authenticateTrainee("John.Smith", "bad");

        assertThrows(AuthenticationException.class, () -> service.getByUsername("John.Smith", "bad"));
        verifyNoInteractions(traineeDao);
    }

    @Test
    void getByUsername_whenTraineeIsMissing_throws() {
        when(traineeDao.findByUsername("John.Smith")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getByUsername("John.Smith", "secret"));
    }

    // ---------- change password ----------

    @Test
    void changePassword_savesNewPassword() {
        givenExistingTrainee(true);

        service.changePassword("John.Smith", "secret", "newPass");

        assertEquals("newPass", savedTrainee().user().password());
    }

    @Test
    void changePassword_withBlankNewPassword_throwsAndSavesNothing() {
        givenExistingTrainee(true);

        assertThrows(IllegalArgumentException.class, () -> service.changePassword("John.Smith", "secret", " "));
        verify(traineeDao, never()).save(any());
    }

    @Test
    void changePassword_withWrongOldPassword_throwsAndSavesNothing() {
        doThrow(new AuthenticationException()).when(authenticationService)
                .authenticateTrainee("John.Smith", "bad");

        assertThrows(AuthenticationException.class, () -> service.changePassword("John.Smith", "bad", "newPass"));
        verify(traineeDao, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void update_changesNamesBirthDateAndAddress_butKeepsCredentials() {
        givenExistingTrainee(true);
        Trainee changes = new Trainee(null, User.of("Johnny", "Smyth"), LocalDate.of(1999, 5, 5), "Almaty");

        service.update("John.Smith", "secret", changes);

        Trainee saved = savedTrainee();
        assertEquals(1L, saved.id());
        assertEquals("Johnny", saved.user().firstName());
        assertEquals("Smyth", saved.user().lastName());
        assertEquals("John.Smith", saved.user().username());
        assertEquals("secret", saved.user().password());
        assertEquals(LocalDate.of(1999, 5, 5), saved.dateOfBirth());
        assertEquals("Almaty", saved.address());
    }

    @Test
    void update_withBlankLastName_throwsAndSavesNothing() {
        givenExistingTrainee(true);
        Trainee changes = new Trainee(null, User.of("Johnny", ""), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.update("John.Smith", "secret", changes));
        verify(traineeDao, never()).save(any());
    }

    // ---------- activate / de-activate ----------

    @Test
    void toggleActive_deactivatesActiveTrainee() {
        givenExistingTrainee(true);

        boolean result = service.toggleActive("John.Smith", "secret");

        assertFalse(result);
        assertFalse(savedTrainee().user().isActive());
    }

    @Test
    void toggleActive_activatesInactiveTrainee() {
        givenExistingTrainee(false);

        boolean result = service.toggleActive("John.Smith", "secret");

        assertTrue(result);
        assertTrue(savedTrainee().user().isActive());
    }

    // ---------- delete ----------

    @Test
    void delete_removesTraineeById() {
        givenExistingTrainee(true);

        service.delete("John.Smith", "secret");

        verify(traineeDao).deleteById(1L);
    }

    @Test
    void delete_withWrongPassword_deletesNothing() {
        doThrow(new AuthenticationException()).when(authenticationService)
                .authenticateTrainee("John.Smith", "bad");

        assertThrows(AuthenticationException.class, () -> service.delete("John.Smith", "bad"));
        verify(traineeDao, never()).deleteById(anyLong());
    }

    // ---------- trainers ----------

    private Trainer trainer(long id, String first, String last) {
        return new Trainer(id, new User(first, last, first + "." + last, "x", true), new TrainingType("YOGA"));
    }

    @Test
    void getNotAssignedTrainers_returnsTrainersFromDao() {
        List<Trainer> trainers = List.of(trainer(5L, "Anna", "Brown"));
        when(trainerDao.findNotAssignedTo("John.Smith")).thenReturn(trainers);

        List<Trainer> result = service.getNotAssignedTrainers("John.Smith", "secret");

        assertSame(trainers, result);
        verify(authenticationService).authenticateTrainee("John.Smith", "secret");
    }

    @Test
    void updateTrainers_assignsFoundTrainers() {
        givenExistingTrainee(true);
        Trainer anna = trainer(5L, "Anna", "Brown");
        when(trainerDao.findByUsernames(Set.of("Anna.Brown"))).thenReturn(List.of(anna));
        when(traineeDao.updateTrainers(1L, Set.of(5L))).thenReturn(List.of(anna));

        List<Trainer> result = service.updateTrainers("John.Smith", "secret", List.of("Anna.Brown"));

        assertEquals(List.of(anna), result);
    }

    @Test
    void updateTrainers_withUnknownTrainer_throwsAndChangesNothing() {
        givenExistingTrainee(true);
        when(trainerDao.findByUsernames(Set.of("nobody"))).thenReturn(List.of());

        assertThrows(NoSuchElementException.class,
                () -> service.updateTrainers("John.Smith", "secret", List.of("nobody")));
        verify(traineeDao, never()).updateTrainers(anyLong(), anySet());
    }

    @Test
    void updateTrainers_withNullList_throws() {
        givenExistingTrainee(true);

        assertThrows(IllegalArgumentException.class, () -> service.updateTrainers("John.Smith", "secret", null));
    }
}
