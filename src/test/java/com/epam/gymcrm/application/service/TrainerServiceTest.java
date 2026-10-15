package com.epam.gymcrm.application.service;

import com.epam.gymcrm.application.port.out.TrainerDao;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.TrainingType;
import com.epam.gymcrm.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    @Mock
    private TrainerDao trainerDao;
    @Mock
    private UsernameGenerator usernameGenerator;
    @Mock
    private PasswordGenerator passwordGenerator;
    @Mock
    private AuthenticationService authenticationService;
    @InjectMocks
    private TrainerService service;

    private void givenExistingTrainer(boolean active) {
        User user = new User("Anna", "Brown", "Anna.Brown", "secret", active);
        when(trainerDao.findByUsername("Anna.Brown"))
                .thenReturn(Optional.of(new Trainer(5L, user, new TrainingType("YOGA"))));
    }

    private Trainer savedTrainer() {
        ArgumentCaptor<Trainer> captor = ArgumentCaptor.forClass(Trainer.class);
        verify(trainerDao).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void create_generatesCredentialsAndSaves() {
        when(usernameGenerator.generate("Anna", "Brown")).thenReturn("Anna.Brown");
        when(passwordGenerator.generate()).thenReturn("pass123456");
        when(trainerDao.save(any(Trainer.class))).thenAnswer(invocation -> {
            Trainer toSave = invocation.getArgument(0);
            return toSave.withId(5L);
        });

        Trainer result = service.create(new Trainer(null, User.of("Anna", "Brown"), new TrainingType("YOGA")));

        assertEquals(5L, result.id());
        assertEquals("Anna.Brown", result.user().username());
        assertEquals("pass123456", result.user().password());
        assertTrue(result.user().isActive());
    }

    @Test
    void create_withoutSpecialization_throwsAndSavesNothing() {
        Trainer invalid = new Trainer(null, User.of("Anna", "Brown"), null);

        assertThrows(IllegalArgumentException.class, () -> service.create(invalid));
        verifyNoInteractions(trainerDao);
    }

    @Test
    void create_withBlankLastName_throws() {
        Trainer invalid = new Trainer(null, User.of("Anna", null), new TrainingType("YOGA"));

        assertThrows(IllegalArgumentException.class, () -> service.create(invalid));
    }

    @Test
    void getByUsername_returnsTrainer() {
        givenExistingTrainer(true);

        Trainer result = service.getByUsername("Anna.Brown", "secret");

        assertEquals(5L, result.id());
        verify(authenticationService).authenticateTrainer("Anna.Brown", "secret");
    }

    @Test
    void getByUsername_withWrongPassword_throwsAndDoesNotTouchDao() {
        doThrow(new AuthenticationException()).when(authenticationService)
                .authenticateTrainer("Anna.Brown", "bad");

        assertThrows(AuthenticationException.class, () -> service.getByUsername("Anna.Brown", "bad"));
        verifyNoInteractions(trainerDao);
    }

    @Test
    void getByUsername_whenTrainerIsMissing_throws() {
        when(trainerDao.findByUsername("Anna.Brown")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getByUsername("Anna.Brown", "secret"));
    }

    @Test
    void changePassword_savesNewPassword() {
        givenExistingTrainer(true);

        service.changePassword("Anna.Brown", "secret", "newPass");

        assertEquals("newPass", savedTrainer().user().password());
    }

    @Test
    void changePassword_withBlankNewPassword_throwsAndSavesNothing() {
        givenExistingTrainer(true);

        assertThrows(IllegalArgumentException.class, () -> service.changePassword("Anna.Brown", "secret", ""));
        verify(trainerDao, never()).save(any());
    }

    @Test
    void update_changesNamesAndSpecialization_butKeepsCredentials() {
        givenExistingTrainer(true);
        Trainer changes = new Trainer(null, User.of("Annie", "Braun"), new TrainingType("FITNESS"));

        service.update("Anna.Brown", "secret", changes);

        Trainer saved = savedTrainer();
        assertEquals(5L, saved.id());
        assertEquals("Annie", saved.user().firstName());
        assertEquals("Braun", saved.user().lastName());
        assertEquals("Anna.Brown", saved.user().username());
        assertEquals("secret", saved.user().password());
        assertEquals("FITNESS", saved.specialization().name());
    }

    @Test
    void update_withoutSpecialization_throwsAndSavesNothing() {
        givenExistingTrainer(true);
        Trainer changes = new Trainer(null, User.of("Annie", "Braun"), null);

        assertThrows(IllegalArgumentException.class, () -> service.update("Anna.Brown", "secret", changes));
        verify(trainerDao, never()).save(any());
    }

    @Test
    void toggleActive_deactivatesActiveTrainer() {
        givenExistingTrainer(true);

        boolean result = service.toggleActive("Anna.Brown", "secret");

        assertFalse(result);
        assertFalse(savedTrainer().user().isActive());
    }

    @Test
    void toggleActive_activatesInactiveTrainer() {
        givenExistingTrainer(false);

        boolean result = service.toggleActive("Anna.Brown", "secret");

        assertTrue(result);
        assertTrue(savedTrainer().user().isActive());
    }
}
