package com.ipt.siga.controllers;

import com.ipt.siga.dtos.SectionCreateRequest;
import com.ipt.siga.dtos.SectionResponse;
import com.ipt.siga.dtos.SectionUpdateRequest;
import com.ipt.siga.services.SectionService;
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
@Tag(name = "Sections", description = "Academic offer: sections, teachers and schedules")
public class SectionController {

    private final SectionService sectionService;

    @Operation(summary = "Create the section of a subject in an academic period")
    @PostMapping("/academic-periods/{periodId}/sections")
    public ResponseEntity<SectionResponse> create(@PathVariable Long periodId,
                                                  @Valid @RequestBody SectionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sectionService.create(periodId, request));
    }

    @Operation(summary = "List the sections of an academic period (optional teacher filter)")
    @GetMapping("/academic-periods/{periodId}/sections")
    public List<SectionResponse> findByPeriod(@PathVariable Long periodId,
                                              @RequestParam(required = false) Long teacherId) {
        return sectionService.findByPeriod(periodId, teacherId);
    }

    @Operation(summary = "Get a section by id")
    @GetMapping("/sections/{id}")
    public SectionResponse findById(@PathVariable Long id) {
        return sectionService.findById(id);
    }

    @Operation(summary = "Update the teacher, capacity and schedule of a section")
    @PutMapping("/sections/{id}")
    public SectionResponse update(@PathVariable Long id, @Valid @RequestBody SectionUpdateRequest request) {
        return sectionService.update(id, request);
    }

    @Operation(summary = "Delete a section")
    @DeleteMapping("/sections/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}