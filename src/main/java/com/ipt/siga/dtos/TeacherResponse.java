package com.ipt.siga.dtos;

import com.ipt.siga.enums.TeacherStatus;

public record TeacherResponse(
        Long id,
        String run,
        String fullName,
        String email,
        String professionalTitle,
        String academicDegree,
        TeacherStatus status
) {}