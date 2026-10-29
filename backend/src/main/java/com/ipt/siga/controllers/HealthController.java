package com.ipt.siga.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Service status")
public class HealthController {

    @Operation(summary = "Check that the service is running")
    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "OK");
    }
}