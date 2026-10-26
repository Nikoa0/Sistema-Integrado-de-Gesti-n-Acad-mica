package com.ipt.siga.dtos;

import com.ipt.siga.enums.TeacherStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherUpdateRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 150) String professionalTitle,
        @NotBlank @Size(max = 150) String academicDegree,
        TeacherStatus status
) {}