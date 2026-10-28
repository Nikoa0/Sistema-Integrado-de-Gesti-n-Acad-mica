package com.ipt.siga.controllers;

import com.ipt.siga.dtos.MeResponse;
import com.ipt.siga.services.AccessControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Me", description = "Identity of the authenticated user")
public class MeController {

    private final AccessControlService accessControlService;

    @Operation(summary = "Roles and linked teacher/student of the authenticated user")
    @GetMapping
    public MeResponse me() {
        return accessControlService.me();
    }
}