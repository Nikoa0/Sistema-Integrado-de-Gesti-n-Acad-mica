package com.ipt.siga.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "academic_periods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcademicPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // Ej: "2027-1" o "2027-2"[cite: 7]

    @Column(nullable = false)
    private String status; // ABIERTO o CERRADO[cite: 7]
}