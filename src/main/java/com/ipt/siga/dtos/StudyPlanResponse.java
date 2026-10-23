package com.ipt.siga.dtos;

import com.ipt.siga.enums.StudyPlanStatus;

public record StudyPlanResponse(
        Long id,
        Long careerId,
        String identifier,
        StudyPlanStatus status
) {}