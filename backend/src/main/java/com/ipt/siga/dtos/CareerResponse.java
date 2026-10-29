package com.ipt.siga.dtos;

import com.ipt.siga.enums.CareerStatus;

public record CareerResponse(
        Long id,
        String code,
        String name,
        String description,
        CareerStatus status,
        Integer durationSemesters
) {}