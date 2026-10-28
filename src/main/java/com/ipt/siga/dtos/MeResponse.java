package com.ipt.siga.dtos;

import java.util.List;

public record MeResponse(String email, List<String> roles, Long teacherId, Long studentId) {}