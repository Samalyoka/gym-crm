package com.epam.gymcrm.infrastructure.persistence;

import com.epam.gymcrm.application.port.out.TraineeDao;
import com.epam.gymcrm.application.port.out.TrainerDao;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.TrainingType;
import com.epam.gymcrm.domain.model.User;
import com.epam.gymcrm.support.JpaTestBase;
import com.epam.gymcrm.support.TestData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainerDaoImplTest extends JpaTestBase {

    private final TrainerDao trainerDao = bean(TrainerDao.class);
    private final TraineeDao traineeDao = bean(TraineeDao.class);

    @Test
    void save_assignsIdAndStoresSpecialization() {
        Trainer saved = tx.execute(status -> trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA")));

        assertNotNull(saved.id());
        Trainer found = tx.execute(status -> trainerDao.findByUsername("Anna.Brown").orElseThrow());
        assertEquals(saved.id(), found.id());
        assertEquals("YOGA", found.specialization().name());
    }

    @Test
    void save_withUnknownSpecialization_throws() {
        assertThrows(NoSuchElementException.class, () ->
                tx.execute(status -> trainerDao.save(TestData.trainer("Anna", "Brown", "BOXING"))));
    }

    @Test
    void save_withExistingId_updatesNamesAndSpecialization() {
        Trainer saved = tx.execute(status -> trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA")));
        Trainer changed = new Trainer(saved.id(), saved.user().withNames("Annie", "Braun"),
                new TrainingType("FITNESS"));

        tx.executeWithoutResult(status -> trainerDao.save(changed));

        Trainer reloaded = tx.execute(status -> trainerDao.findById(saved.id()).orElseThrow());
        assertEquals("Annie", reloaded.user().firstName());
        assertEquals("FITNESS", reloaded.specialization().name());
        assertEquals(1, count("users"));
    }

    @Test
    void save_withUnknownId_throws() {
        Trainer unknown = new Trainer(999L, User.of("A", "B").withCredentials("A.B", "p"),
                new TrainingType("YOGA"));

        assertThrows(NoSuchElementException.class, () -> tx.execute(status -> trainerDao.save(unknown)));
    }

    @Test
    void findById_findAll_andUnknownLookups() {
        tx.executeWithoutResult(status -> {
            trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA"));
            trainerDao.save(TestData.trainer("Bob", "White", "FITNESS"));
        });

        assertEquals(2, tx.execute(status -> trainerDao.findAll()).size());
        assertTrue(tx.execute(status -> trainerDao.findById(999L)).isEmpty());
        assertTrue(tx.execute(status -> trainerDao.findByUsername("nobody")).isEmpty());
    }

    @Test
    void findByUsernames_returnsOnlyRequestedTrainers() {
        tx.executeWithoutResult(status -> {
            trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA"));
            trainerDao.save(TestData.trainer("Bob", "White", "FITNESS"));
            trainerDao.save(TestData.trainer("Carl", "Black", "YOGA"));
        });

        List<Trainer> result = tx.execute(status ->
                trainerDao.findByUsernames(List.of("Anna.Brown", "Carl.Black")));

        assertEquals(2, result.size());
        assertEquals(Set.of("Anna.Brown", "Carl.Black"),
                Set.of(result.get(0).user().username(), result.get(1).user().username()));
    }

    @Test
    void findByUsernames_withEmptyCollection_returnsEmptyList() {
        assertTrue(tx.execute(status -> trainerDao.findByUsernames(List.of())).isEmpty());
    }

    @Test
    void findNotAssignedTo_excludesTrainersAssignedToTheTrainee() {
        tx.executeWithoutResult(status -> {
            Trainee trainee = traineeDao.save(TestData.trainee("John", "Smith"));
            Trainer anna = trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA"));
            trainerDao.save(TestData.trainer("Bob", "White", "FITNESS"));
            traineeDao.updateTrainers(trainee.id(), Set.of(anna.id()));
        });

        List<Trainer> result = tx.execute(status -> trainerDao.findNotAssignedTo("John.Smith"));

        assertEquals(1, result.size());
        assertEquals("Bob.White", result.get(0).user().username());
    }

    @Test
    void findNotAssignedTo_withTraineeWithoutTrainers_returnsAllTrainers() {
        tx.executeWithoutResult(status -> {
            traineeDao.save(TestData.trainee("John", "Smith"));
            trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA"));
            trainerDao.save(TestData.trainer("Bob", "White", "FITNESS"));
        });

        assertEquals(2, tx.execute(status -> trainerDao.findNotAssignedTo("John.Smith")).size());
    }
}
