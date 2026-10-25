package com.ipt.siga.dtos;

import com.ipt.siga.enums.StudentStatus;

public record StudentResponse(
        Long id,
        String run,
        String fullName,
        String email,
        Long careerId,
        String careerName,
        Long studyPlanId,
        String studyPlanIdentifier,
        StudentStatus status
) {}