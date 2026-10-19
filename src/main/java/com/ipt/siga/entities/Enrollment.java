package com.ipt.siga.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double finalGrade; // Rango de 1.0 a 7.0 (Puede ser null si aún no termina el semestre)[cite: 10]

    private LocalDate registrationDate; // Fecha en que se realizó la inscripción[cite: 9]

    // Relaciones de la inscripción[cite: 9]
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne
    @JoinColumn(name = "period_id", nullable = false)
    private AcademicPeriod academicPeriod;
}