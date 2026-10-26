package com.ipt.siga.dtos;

import java.util.List;

public record SectionResponse(
        Long id,
        Long periodId,
        String periodName,
        Long subjectId,
        String subjectCode,
        String subjectName,
        Long teacherId,
        String teacherName,
        Integer capacity,
        List<String> blocks
) {}