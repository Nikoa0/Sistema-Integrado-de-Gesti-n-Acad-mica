package com.ipt.siga.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer capacity; // No podrá superar los 60 estudiantes[cite: 8]

    // Guarda una lista de bloques horarios simples como "L2", "W3"[cite: 7, 8]
    @ElementCollection
    @CollectionTable(name = "section_schedules", joinColumns = @JoinColumn(name = "section_id"))
    @Column(name = "schedule_block")
    private List<String> scheduleBlocks;

    // Relaciones de la sección[cite: 8]
    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "period_id", nullable = false)
    private AcademicPeriod academicPeriod;
}