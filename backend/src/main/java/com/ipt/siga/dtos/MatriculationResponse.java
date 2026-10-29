package com.ipt.siga.dtos;

public record MatriculationResponse(
        Long id,
        Long studentId,
        String studentName,
        Long periodId,
        String periodName
) {}