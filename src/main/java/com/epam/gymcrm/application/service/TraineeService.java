package com.epam.gymcrm.application.service;

import com.epam.gymcrm.application.port.out.TraineeDao;
import com.epam.gymcrm.application.port.out.TrainerDao;
import com.epam.gymcrm.domain.model.Trainee;
import com.epam.gymcrm.domain.model.Trainer;
import com.epam.gymcrm.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeService.class);

    private TraineeDao traineeDao;
    private TrainerDao trainerDao;
    private UsernameGenerator usernameGenerator;
    private PasswordGenerator passwordGenerator;
    private AuthenticationService authenticationService;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Autowired
    public void setUsernameGenerator(UsernameGenerator usernameGenerator) {
        this.usernameGenerator = usernameGenerator;
    }

    @Autowired
    public void setPasswordGenerator(PasswordGenerator passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
    }

    @Autowired
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /** Creates a trainee profile; username and password are generated. */
    @Transactional
    public Trainee create(Trainee trainee) {
        Validation.requireNotNull(trainee, "trainee");
        Validation.requireUser(trainee.user());
        User user = trainee.user();
        String username = usernameGenerator.generate(user.firstName(), user.lastName());
        String password = passwordGenerator.generate();
        Trainee saved = traineeDao.save(trainee.withUser(user.withCredentials(username, password)));
        log.info("Created trainee id={}, username={}", saved.id(), username);
        return saved;
    }

    @Transactional(readOnly = true)
    public Trainee getByUsername(String username, String password) {
        return authenticated(username, password);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        Trainee trainee = authenticated(username, oldPassword);
        Validation.requireText(newPassword, "newPassword");
        traineeDao.save(trainee.withUser(trainee.user().withPassword(newPassword)));
        log.info("Password changed for trainee username={}", username);
    }

    /** Updates names, date of birth and address; username and password stay the same. */
    @Transactional
    public Trainee update(String username, String password, Trainee changes) {
        Trainee existing = authenticated(username, password);
        Validation.requireNotNull(changes, "trainee");
        Validation.requireUser(changes.user());
        User user = existing.user().withNames(changes.user().firstName(), changes.user().lastName());
        Trainee saved = traineeDao.save(
                new Trainee(existing.id(), user, changes.dateOfBirth(), changes.address()));
        log.info("Updated trainee username={}", username);
        return saved;
    }

    /** Flips the active flag on every call (not idempotent) and returns the new value. */
    @Transactional
    public boolean toggleActive(String username, String password) {
        Trainee trainee = authenticated(username, password);
        boolean active = !trainee.user().isActive();
        traineeDao.save(trainee.withUser(trainee.user().withActive(active)));
        log.info("Trainee username={} is now active={}", username, active);
        return active;
    }

    /** Deletes the trainee; trainings are deleted by cascade. */
    @Transactional
    public void delete(String username, String password) {
        Trainee trainee = authenticated(username, password);
        traineeDao.deleteById(trainee.id());
        log.info("Deleted trainee username={}", username);
    }

    @Transactional(readOnly = true)
    public List<Trainer> getNotAssignedTrainers(String username, String password) {
        authenticationService.authenticateTrainee(username, password);
        return trainerDao.findNotAssignedTo(username);
    }

    /** Replaces the trainee's trainers with the given ones. */
    @Transactional
    public List<Trainer> updateTrainers(String username, String password, List<String> trainerUsernames) {
        Trainee trainee = authenticated(username, password);
        Validation.requireNotNull(trainerUsernames, "trainerUsernames");

        Set<String> requested = new HashSet<>(trainerUsernames);
        List<Trainer> trainers = trainerDao.findByUsernames(requested);
        if (trainers.size() != requested.size()) {
            Set<String> found = trainers.stream().map(t -> t.user().username()).collect(Collectors.toSet());
            requested.removeAll(found);
            throw new NoSuchElementException("Trainers not found: " + requested);
        }
        Set<Long> ids = trainers.stream().map(Trainer::id).collect(Collectors.toSet());
        List<Trainer> updated = traineeDao.updateTrainers(trainee.id(), ids);
        log.info("Updated trainers of trainee username={}, count={}", username, updated.size());
        return updated;
    }

    private Trainee authenticated(String username, String password) {
        authenticationService.authenticateTrainee(username, password);
        return traineeDao.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Trainee not found: " + username));
    }
}
