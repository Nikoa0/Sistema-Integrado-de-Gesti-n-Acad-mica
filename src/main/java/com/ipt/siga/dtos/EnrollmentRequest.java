package com.ipt.siga.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EnrollmentRequest(
        @NotNull Long periodId,
        @NotEmpty List<@NotNull Long> subjectIds
) {}