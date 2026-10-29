package com.ipt.siga.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record GradeRequest(
        @NotNull @DecimalMin("1.0") @DecimalMax("7.0") Double grade
) {}