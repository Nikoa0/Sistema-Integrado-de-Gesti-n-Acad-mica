package com.ipt.siga.controllers;

import com.ipt.siga.dtos.AcademicPeriodRequest;
import com.ipt.siga.dtos.AcademicPeriodResponse;
import com.ipt.siga.services.AcademicPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-periods")
@RequiredArgsConstructor
@Tag(name = "Academic periods", description = "Management of academic periods")
public class AcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;

    @Operation(summary = "Create an academic period (it starts OPEN)")
    @PostMapping
    public ResponseEntity<AcademicPeriodResponse> create(@Valid @RequestBody AcademicPeriodRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(academicPeriodService.create(request));
    }

    @Operation(summary = "List academic periods, newest first")
    @GetMapping
    public List<AcademicPeriodResponse> findAll() {
        return academicPeriodService.findAll();
    }

    @Operation(summary = "Get an academic period by id")
    @GetMapping("/{id}")
    public AcademicPeriodResponse findById(@PathVariable Long id) {
        return academicPeriodService.findById(id);
    }

    @Operation(summary = "Delete an academic period (only if it has no sections)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        academicPeriodService.delete(id);
        return ResponseEntity.noContent().build();
    }
}