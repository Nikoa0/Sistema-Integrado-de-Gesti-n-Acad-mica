package com.ipt.siga.dtos;

import jakarta.validation.constraints.NotNull;

public record MatriculationRequest(
        @NotNull Long studentId
) {}