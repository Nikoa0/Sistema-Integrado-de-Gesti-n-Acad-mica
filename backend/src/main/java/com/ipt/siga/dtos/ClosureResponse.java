package com.ipt.siga.dtos;

import com.ipt.siga.enums.PeriodStatus;

import java.util.List;

public record ClosureResponse(
        Long periodId,
        String periodName,
        PeriodStatus status,
        List<String> graduatedRuns,
        List<String> eliminatedRuns
) {}