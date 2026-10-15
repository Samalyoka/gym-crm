package com.epam.gymcrm.infrastructure.persistence;

import com.epam.gymcrm.application.port.out.TraineeDao;
import com.epam.gymcrm.application.port.out.TrainerDao;
import com.epam.gymcrm.application.port.out.TrainingDao;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.Training;
import com.epam.gymcrm.domain.model.TrainingType;
import com.epam.gymcrm.support.JpaTestBase;
import com.epam.gymcrm.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingDaoImplTest extends JpaTestBase {

    private final TraineeDao traineeDao = bean(TraineeDao.class);
    private final TrainerDao trainerDao = bean(TrainerDao.class);
    private final TrainingDao trainingDao = bean(TrainingDao.class);

    /**
     * John trains with Anna (yoga) twice and with Bob (fitness) once; Mike trains with Anna once.
     */
    @BeforeEach
    void seed() {
        tx.executeWithoutResult(status -> {
            Trainee john = traineeDao.save(TestData.trainee("John", "Smith"));
            Trainee mike = traineeDao.save(TestData.trainee("Mike", "Jones"));
            Trainer anna = trainerDao.save(TestData.trainer("Anna", "Brown", "YOGA"));
            Trainer bob = trainerDao.save(TestData.trainer("Bob", "White", "FITNESS"));
            trainingDao.save(training(john, anna, "Yoga basics", "YOGA", LocalDate.of(2026, 1, 10), 60));
            trainingDao.save(training(mike, anna, "Yoga for Mike", "YOGA", LocalDate.of(2026, 1, 20), 45));
            trainingDao.save(training(john, bob, "Cardio", "FITNESS", LocalDate.of(2026, 2, 10), 45));
            trainingDao.save(training(john, anna, "Yoga advanced", "YOGA", LocalDate.of(2026, 3, 10), 90));
        });
    }

    private Training training(Trainee trainee, Trainer trainer, String name, String type,
                              LocalDate date, int minutes) {
        return new Training(null, trainee.id(), trainer.id(), name, new TrainingType(type), date, minutes);
    }

    private List<String> names(List<Training> trainings) {
        return trainings.stream().map(Training::trainingName).toList();
    }

    // ---------- save / find ----------

    @Test
    void save_persistsTraining_andFindByIdReturnsIt() {
        Long id = tx.execute(status -> {
            Trainee john = traineeDao.findByUsername("John.Smith").orElseThrow();
            Trainer bob = trainerDao.findByUsername("Bob.White").orElseThrow();
            return trainingDao.save(training(john, bob, "Stretching", "YOGA", LocalDate.of(2026, 4, 1), 30)).id();
        });

        Training found = tx.execute(status -> trainingDao.findById(id).orElseThrow());

        assertEquals("Stretching", found.trainingName());
        assertEquals("YOGA", found.trainingType().name());
        assertEquals(30, found.trainingDuration());
        assertEquals(LocalDate.of(2026, 4, 1), found.trainingDate());
        assertEquals(5, count("training"));
    }

    @Test
    void save_withUnknownTrainee_throws() {
        assertThrows(NoSuchElementException.class, () -> tx.execute(status -> {
            Trainer bob = trainerDao.findByUsername("Bob.White").orElseThrow();
            return trainingDao.save(new Training(null, 999L, bob.id(), "X",
                    new TrainingType("YOGA"), LocalDate.now(), 30));
        }));
    }

    @Test
    void save_withUnknownTrainer_throws() {
        assertThrows(NoSuchElementException.class, () -> tx.execute(status -> {
            Trainee john = traineeDao.findByUsername("John.Smith").orElseThrow();
            return trainingDao.save(new Training(null, john.id(), 999L, "X",
                    new TrainingType("YOGA"), LocalDate.now(), 30));
        }));
    }

    @Test
    void save_withUnknownTrainingType_throws() {
        assertThrows(NoSuchElementException.class, () -> tx.execute(status -> {
            Trainee john = traineeDao.findByUsername("John.Smith").orElseThrow();
            Trainer bob = trainerDao.findByUsername("Bob.White").orElseThrow();
            return trainingDao.save(training(john, bob, "X", "BOXING", LocalDate.now(), 30));
        }));
    }

    @Test
    void save_withExistingId_isNotSupported() {
        Training existing = new Training(1L, 1L, 1L, "X", new TrainingType("YOGA"), LocalDate.now(), 30);

        assertThrows(IllegalArgumentException.class, () -> tx.execute(status -> trainingDao.save(existing)));
    }

    @Test
    void findById_withUnknownId_returnsEmpty() {
        assertTrue(tx.execute(status -> trainingDao.findById(999L)).isEmpty());
    }

    @Test
    void findAll_returnsAllTrainings() {
        assertEquals(4, tx.execute(status -> trainingDao.findAll()).size());
    }

    // ---------- trainee trainings ----------

    @Test
    void findForTrainee_withoutFilters_returnsAllOrderedByDate() {
        List<Training> result = tx.execute(status ->
                trainingDao.findForTrainee("John.Smith", null, null, null, null));

        assertEquals(List.of("Yoga basics", "Cardio", "Yoga advanced"), names(result));
    }

    @Test
    void findForTrainee_filtersByDateRange_inclusively() {
        List<Training> february = tx.execute(status -> trainingDao.findForTrainee(
                "John.Smith", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), null, null));
        List<Training> exactDay = tx.execute(status -> trainingDao.findForTrainee(
                "John.Smith", LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 10), null, null));

        assertEquals(List.of("Cardio"), names(february));
        assertEquals(List.of("Cardio"), names(exactDay));
    }

    @Test
    void findForTrainee_filtersByTrainerName_ignoringCase() {
        List<Training> result = tx.execute(status ->
                trainingDao.findForTrainee("John.Smith", null, null, "anna", null));

        assertEquals(List.of("Yoga basics", "Yoga advanced"), names(result));
    }

    @Test
    void findForTrainee_filtersByTrainingType() {
        List<Training> result = tx.execute(status ->
                trainingDao.findForTrainee("John.Smith", null, null, null, "FITNESS"));

        assertEquals(List.of("Cardio"), names(result));
    }

    @Test
    void findForTrainee_combinesFilters() {
        List<Training> result = tx.execute(status -> trainingDao.findForTrainee(
                "John.Smith", LocalDate.of(2026, 2, 1), null, "Anna", "YOGA"));

        assertEquals(List.of("Yoga advanced"), names(result));
    }

    @Test
    void findForTrainee_ignoresBlankFilters() {
        List<Training> result = tx.execute(status ->
                trainingDao.findForTrainee("John.Smith", null, null, "  ", ""));

        assertEquals(3, result.size());
    }

    @Test
    void findForTrainee_withUnknownUsername_returnsEmptyList() {
        assertTrue(tx.execute(status ->
                trainingDao.findForTrainee("nobody", null, null, null, null)).isEmpty());
    }

    // ---------- trainer trainings ----------

    @Test
    void findForTrainer_withoutFilters_returnsAllOrderedByDate() {
        List<Training> result = tx.execute(status ->
                trainingDao.findForTrainer("Anna.Brown", null, null, null));

        assertEquals(List.of("Yoga basics", "Yoga for Mike", "Yoga advanced"), names(result));
    }

    @Test
    void findForTrainer_filtersByTraineeName_ignoringCase() {
        List<Training> result = tx.execute(status ->
                trainingDao.findForTrainer("Anna.Brown", null, null, "mike"));

        assertEquals(List.of("Yoga for Mike"), names(result));
    }

    @Test
    void findForTrainer_filtersByDateRange() {
        List<Training> result = tx.execute(status -> trainingDao.findForTrainer(
                "Anna.Brown", LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 31), null));

        assertEquals(List.of("Yoga for Mike", "Yoga advanced"), names(result));
    }

    @Test
    void findForTrainer_withUnknownUsername_returnsEmptyList() {
        assertTrue(tx.execute(status ->
                trainingDao.findForTrainer("nobody", null, null, null)).isEmpty());
    }
}
