package com.ipt.siga.dtos;

import java.util.List;

public record SubjectResponse(
        Long id,
        Long studyPlanId,
        String code,
        String name,
        Integer semester,
        Integer theoryHours,
        Integer exerciseHours,
        Integer labHours,
        Integer sct,
        List<String> prerequisiteCodes
) {}