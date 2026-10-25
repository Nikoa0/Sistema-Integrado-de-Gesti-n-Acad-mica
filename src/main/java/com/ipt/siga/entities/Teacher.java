package com.ipt.siga.entities;

import com.ipt.siga.enums.TeacherStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String run;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 150)
    private String professionalTitle;

    @Column(nullable = false, length = 150)
    private String academicDegree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherStatus status;
}