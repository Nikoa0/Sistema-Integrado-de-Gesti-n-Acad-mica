package com.ipt.siga.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code; // Su código debe ser único dentro del plan[cite: 6]

    @Column(nullable = false)
    private String name;

    private Integer semester; // Entre 1 y 4[cite: 6]

    // Componentes del TEL[cite: 6]
    private Integer theoryHours;
    private Integer exerciseHours;
    private Integer labHours;

    private Integer sct; // Créditos[cite: 6]

    @ManyToOne
    @JoinColumn(name = "study_plan_id", nullable = false)
    private StudyPlan studyPlan;

    // Relación: Una asignatura puede tener de 0 a 3 asignaturas como prerrequisito[cite: 6]
    @ManyToMany
    @JoinTable(
            name = "subject_prerequisites",
            joinColumns = @JoinColumn(name = "subject_id"),
            inverseJoinColumns = @JoinColumn(name = "prerequisite_id")
    )
    private List<Subject> prerequisites;
}