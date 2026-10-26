package com.ipt.siga.dtos;

import java.time.LocalDate;

public record EnrollmentResponse(
        Long id,
        Long studentId,
        String studentName,
        Long subjectId,
        String subjectCode,
        String subjectName,
        Long sectionId,
        Long periodId,
        String periodName,
        LocalDate enrolledAt,
        Double grade
) {}