package com.epam.gymcrm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "training")
public class TrainingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private TraineeEntity trainee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private TrainerEntity trainer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "training_type_id", nullable = false)
    private TrainingTypeEntity trainingType;

    @Column(name = "training_name", nullable = false)
    private String name;

    @Column(name = "training_date", nullable = false)
    private LocalDate date;

    @Column(name = "training_duration", nullable = false)
    private Integer duration;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TraineeEntity getTrainee() { return trainee; }
    public void setTrainee(TraineeEntity trainee) { this.trainee = trainee; }

    public TrainerEntity getTrainer() { return trainer; }
    public void setTrainer(TrainerEntity trainer) { this.trainer = trainer; }

    public TrainingTypeEntity getTrainingType() { return trainingType; }
    public void setTrainingType(TrainingTypeEntity trainingType) { this.trainingType = trainingType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}
