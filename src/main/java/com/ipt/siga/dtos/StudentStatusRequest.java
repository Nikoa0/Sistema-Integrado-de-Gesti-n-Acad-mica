package com.ipt.siga.dtos;

import com.ipt.siga.enums.StudentStatus;
import jakarta.validation.constraints.NotNull;

public record StudentStatusRequest(
        @NotNull StudentStatus status
) {}