package com.ipt.siga.dtos;

import com.ipt.siga.enums.TeacherStatus;
import jakarta.validation.constraints.NotNull;

public record TeacherStatusRequest(
        @NotNull TeacherStatus status
) {}