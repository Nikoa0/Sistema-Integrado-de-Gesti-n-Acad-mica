package com.ipt.siga.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String run; // El RUN debe ser único[cite: 7]

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email; // El correo debe ser único[cite: 7]

    private String title;
    private String academicDegree;

    @Column(nullable = false)
    private String status; // ACTIVO o INACTIVO[cite: 7]
}