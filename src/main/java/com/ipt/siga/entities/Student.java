package com.ipt.siga.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String run; // El RUN debe ser único[cite: 5]

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email; // El correo debe ser único[cite: 5]

    @Column(nullable = false)
    private String status; // REGULAR, EGRESADO, ELIMINADO, etc.[cite: 4]

    // Relación: Todo estudiante debe estar asociado a una carrera y un plan de estudios[cite: 5]
    @ManyToOne
    @JoinColumn(name = "career_id", nullable = false)
    private Career career;

    @ManyToOne
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;
}   