package com.ipt.siga.dtos;

import com.ipt.siga.enums.PeriodStatus;

public record AcademicPeriodResponse(
        Long id,
        Integer year,
        Integer semester,
        String name,
        PeriodStatus status
) {}