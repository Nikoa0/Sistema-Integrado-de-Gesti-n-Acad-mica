package com.ipt.siga.dtos;

import jakarta.validation.constraints.NotNull;

public record AcademicPeriodRequest(
        @NotNull Integer year,
        @NotNull Integer semester
) {}