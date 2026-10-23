package com.ipt.siga.entities;

import com.ipt.siga.enums.StudyPlanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "study_plans",
        uniqueConstraints = @UniqueConstraint(columnNames = {"career_id", "identifier"}))
@Getter
@Setter
@NoArgsConstructor
public class StudyPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "career_id", nullable = false)
    private Career career;

    @Column(nullable = false, length = 20)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyPlanStatus status;
}