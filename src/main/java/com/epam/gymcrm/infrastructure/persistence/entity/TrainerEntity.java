package com.epam.gymcrm.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trainer")
public class TrainerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "specialization_id", nullable = false)
    private TrainingTypeEntity specialization;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @ManyToMany(mappedBy = "trainers")
    private Set<TraineeEntity> trainees = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TrainingTypeEntity getSpecialization() { return specialization; }
    public void setSpecialization(TrainingTypeEntity specialization) { this.specialization = specialization; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public Set<TraineeEntity> getTrainees() { return trainees; }
    public void setTrainees(Set<TraineeEntity> trainees) { this.trainees = trainees; }
}
