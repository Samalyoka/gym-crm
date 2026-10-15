package com.epam.gymcrm.application.service;

import com.epam.gymcrm.application.port.out.TraineeDao;
import com.epam.gymcrm.application.port.out.TrainerDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private TraineeDao traineeDao;
    private TrainerDao trainerDao;

    @Autowired
    public void setTraineeDao(TraineeDao traineeDao) {
        this.traineeDao = traineeDao;
    }

    @Autowired
    public void setTrainerDao(TrainerDao trainerDao) {
        this.trainerDao = trainerDao;
    }

    @Transactional(readOnly = true)
    public boolean matchesTrainee(String username, String password) {
        return traineeDao.findByUsername(username)
                .map(trainee -> passwordMatches(trainee.user().password(), password))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean matchesTrainer(String username, String password) {
        return trainerDao.findByUsername(username)
                .map(trainer -> passwordMatches(trainer.user().password(), password))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void authenticateTrainee(String username, String password) {
        if (!matchesTrainee(username, password)) {
            log.warn("Trainee authentication failed for username={}", username);
            throw new AuthenticationException();
        }
    }

    @Transactional(readOnly = true)
    public void authenticateTrainer(String username, String password) {
        if (!matchesTrainer(username, password)) {
            log.warn("Trainer authentication failed for username={}", username);
            throw new AuthenticationException();
        }
    }

    /** Constant-time comparison, so the response time does not reveal how many characters matched. */
    private boolean passwordMatches(String stored, String given) {
        if (stored == null || given == null) {
            return false;
        }
        return MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }
}
