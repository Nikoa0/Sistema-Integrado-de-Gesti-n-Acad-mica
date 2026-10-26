package com.ipt.siga.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record SectionCreateRequest(
        @NotNull Long subjectId,
        @NotNull Long teacherId,
        @NotNull @Min(1) @Max(60) Integer capacity,
        @NotEmpty List<@NotBlank @Pattern(regexp = "^[LMWJV][1-6]$",
                message = "each block must be a day (L, M, W, J, V) followed by a module from 1 to 6, e.g. L2") String> blocks
) {}