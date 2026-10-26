package com.ipt.siga.entities;

import com.ipt.siga.enums.PeriodStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "academic_periods",
        uniqueConstraints = @UniqueConstraint(columnNames = {"academic_year", "semester"}))
@Getter
@Setter
@NoArgsConstructor
public class AcademicPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    @Column(nullable = false)
    private Integer semester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodStatus status;
}