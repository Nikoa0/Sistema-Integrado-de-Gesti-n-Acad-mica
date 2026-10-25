package com.ipt.siga.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentCreateRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{7,8}-[\\dkK]$", message = "RUN must have the format 12345678-9")
        String run,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull Long careerId
) {}