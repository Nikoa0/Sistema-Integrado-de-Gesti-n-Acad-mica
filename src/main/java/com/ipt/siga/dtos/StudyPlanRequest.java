package com.ipt.siga.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyPlanRequest(
        @NotBlank @Size(max = 20) String identifier,
        Boolean current
) {}