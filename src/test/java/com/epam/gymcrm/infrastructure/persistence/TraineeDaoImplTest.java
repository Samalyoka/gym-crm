package com.epam.gymcrm.infrastructure.persistence;

import com.epam.gymcrm.application.port.out.TraineeDao;
import com.epam.gymcrm.application.port.out.TrainerDao;
import com.epam.gymcrm.application.port.out.TrainingDao;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.Training;
import com.epam.gymcrm.domain.model.TrainingType;
import com.epam.gymcrm.domain.model.User;
import com.epam.gymcrm.support.JpaTestBase;
import com.epam.gymcrm.support.TestData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraineeDaoImplTest extends JpaTestBase {

    private final TraineeDao traineeDao = bean(TraineeDao.class);
    private final TrainerDao trainerDao = bean(TrainerDao.class);
    private final TrainingDao trainingDao = bean(TrainingDao.class);

    @Test
    void save_assignsIdAndCanBeFoundByUsername() {
        Trainee saved = tx.execute(status -> traineeDao.save(TestData.trainee("John", "Smith")));

        assertNotNull(saved.id());
        Trainee found = tx.execute(status -> traineeDao.findByUsername("John.Smith").orElseThrow());
        assertEquals(saved.id(), found.id());
        assertEquals("Astana", found.address());
        assertEquals(LocalDate.of(2000, 1, 1), found.dateOfBirth());
    }

    @Test
    void save_withExistingId_updatesFields() {
        Trainee saved = tx.execute(status -> traineeDao.save(TestData.trainee("John", "Smith")));
        Trainee changed = new Trainee(saved.id(), saved.user().withNames("Johnny", "Smyth"),
                LocalDate.of(1999, 5, 5), "Almaty");

        tx.executeWithoutResult(status -> traineeDao.save(changed));

        Trainee reloaded = tx.execute(status -> traineeDao.findById(saved.id()).orElseThrow());
        assertEquals("Johnny", reloaded.user().firstName());
        assertEquals("Almaty", reloaded.address());
        assertEquals(LocalDate.of(1999, 5, 5), reloaded.dateOfBirth());
        assertEquals(1, count("users"));
    }

    @Test
    void save_withUnknownId_throws() {
        Trainee unknown = new Trainee(999L, User.of("A", "B").withCredentials("A.B", "p"), null, null);

        assertThrows(NoSuchElementException.class, () -> tx.execute(status -> traineeDao.save(unknown)));
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        assertTrue(tx.execute(status -> traineeDao.findById(999L)).isEmpty());
    }

    @Test
    void findByUsername_returnsEmptyForUnknownUsername() {
        assertTrue(tx.execute(status -> traineeDao.findByUsername("nobody")).isEmpty());
    }

    @Test
    void findAll_returnsAllTrainees() {
        tx.executeWithoutResult(status -> {
            traineeDao.save(TestData.trainee("John", "Smith"));
            traineeDao.save(TestData.trainee("Mike", "Jones"));
        });

        assertEquals(2, tx.execute(status -> traineeDao.findAll()).size());
    }

    @Test
    void findUsernamesStartingWith_looksThroughAllUsers() {
        tx.executeWithoutResult(status -> {
            traineeDao.save(TestData.trainee("John", "Smith"));
            traineeDao.save(new Trainee(null,
                    User.of("John", "Smith").withCredentials("John.Smith1", "p"), null, null));
            trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA"));
        });

        List<String> usernames = tx.execute(status -> traineeDao.findUsernamesStartingWith("John.Smith"));

        assertEquals(Set.of("John.Smith", "John.Smith1"), new HashSet<>(usernames));
    }

    @Test
    void updateTrainers_replacesAssignedTrainers() {
        Long traineeId = tx.execute(status -> traineeDao.save(TestData.trainee("John", "Smith")).id());
        List<Long> trainerIds = tx.execute(status -> List.of(
                trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA")).id(),
                trainerDao.save(TestData.trainer("Bob", "White", "FITNESS")).id()));

        tx.executeWithoutResult(status ->
                traineeDao.updateTrainers(traineeId, Set.of(trainerIds.get(0), trainerIds.get(1))));
        assertEquals(2, count("trainee_trainer"));

        List<Trainer> result = tx.execute(status ->
                traineeDao.updateTrainers(traineeId, Set.of(trainerIds.get(0))));

        assertEquals(1, result.size());
        assertEquals("Anna.Brown", result.get(0).user().username());
        assertEquals(1, count("trainee_trainer"));
    }

    @Test
    void updateTrainers_withEmptySet_removesAllAssignments() {
        Long traineeId = tx.execute(status -> traineeDao.save(TestData.trainee("John", "Smith")).id());
        Long trainerId = tx.execute(status -> trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA")).id());
        tx.executeWithoutResult(status -> traineeDao.updateTrainers(traineeId, Set.of(trainerId)));

        List<Trainer> result = tx.execute(status -> traineeDao.updateTrainers(traineeId, Set.of()));

        assertTrue(result.isEmpty());
        assertEquals(0, count("trainee_trainer"));
    }

    @Test
    void updateTrainers_withUnknownTrainer_throwsAndChangesNothing() {
        Long traineeId = tx.execute(status -> traineeDao.save(TestData.trainee("John", "Smith")).id());

        assertThrows(NoSuchElementException.class, () ->
                tx.execute(status -> traineeDao.updateTrainers(traineeId, Set.of(999L))));

        assertEquals(0, count("trainee_trainer"));
    }

    @Test
    void updateTrainers_withUnknownTrainee_throws() {
        assertThrows(NoSuchElementException.class, () ->
                tx.execute(status -> traineeDao.updateTrainers(999L, Set.of())));
    }

    @Test
    void deleteById_removesTraineeTrainingsAndUserButKeepsTrainer() {
        Long traineeId = tx.execute(status -> {
            Trainee trainee = traineeDao.save(TestData.trainee("John", "Smith"));
            Trainer trainer = trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA"));
            trainingDao.save(new Training(null, trainee.id(), trainer.id(), "Session",
                    new TrainingType("YOGA"), LocalDate.now(), 60));
            return trainee.id();
        });
        assertEquals(1, count("training"));

        tx.executeWithoutResult(status -> traineeDao.deleteById(traineeId));

        assertEquals(0, count("trainee"));
        assertEquals(0, count("training"));      // removed by cascade
        assertEquals(1, count("trainer"));       // the trainer stays
        assertEquals(1, count("users"));         // only the trainer's user is left
        assertTrue(tx.execute(status -> traineeDao.findByUsername("John.Smith")).isEmpty());
    }

    @Test
    void deleteById_withUnknownId_doesNothing() {
        tx.executeWithoutResult(status -> traineeDao.deleteById(999L));

        assertEquals(0, count("trainee"));
    }
}
