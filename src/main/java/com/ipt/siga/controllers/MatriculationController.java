package com.ipt.siga.controllers;

import com.ipt.siga.dtos.MatriculationRequest;
import com.ipt.siga.dtos.MatriculationResponse;
import com.ipt.siga.services.MatriculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Matriculations", description = "Students matriculated in each academic period")
public class MatriculationController {

    private final MatriculationService matriculationService;

    @Operation(summary = "Register a student as matriculated in an academic period")
    @PostMapping("/academic-periods/{periodId}/matriculations")
    public ResponseEntity<MatriculationResponse> create(@PathVariable Long periodId,
                                                        @Valid @RequestBody MatriculationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matriculationService.create(periodId, request));
    }

    @Operation(summary = "List the students matriculated in an academic period")
    @GetMapping("/academic-periods/{periodId}/matriculations")
    public List<MatriculationResponse> findByPeriod(@PathVariable Long periodId) {
        return matriculationService.findByPeriod(periodId);
    }

    @Operation(summary = "Remove a matriculation")
    @DeleteMapping("/matriculations/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        matriculationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}