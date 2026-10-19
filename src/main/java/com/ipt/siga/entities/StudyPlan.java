package com.ipt.siga.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "study_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false)
    private String status;

    // Relación: Muchos planes de estudio pertenecen a una Carrera[cite: 5, 6]
    @ManyToOne
    @JoinColumn(name = "career_id", nullable = false)
    private Career career;
}