package com.ipt.siga.controllers;

import com.ipt.siga.dtos.SubjectRequest;
import com.ipt.siga.dtos.SubjectResponse;
import com.ipt.siga.services.SubjectService;
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
@Tag(name = "Subjects", description = "Management of subjects and prerequisites")
public class SubjectController {

    private final SubjectService subjectService;

    @Operation(summary = "Create a subject in a study plan")
    @PostMapping("/study-plans/{planId}/subjects")
    public ResponseEntity<SubjectResponse> create(@PathVariable Long planId,
                                                  @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.create(planId, request));
    }

    @Operation(summary = "List the subjects of a study plan (optional semester filter)")
    @GetMapping("/study-plans/{planId}/subjects")
    public List<SubjectResponse> findByPlan(@PathVariable Long planId,
                                            @RequestParam(required = false) Integer semester) {
        return subjectService.findByPlan(planId, semester);
    }

    @Operation(summary = "Get a subject by id")
    @GetMapping("/subjects/{id}")
    public SubjectResponse findById(@PathVariable Long id) {
        return subjectService.findById(id);
    }

    @Operation(summary = "Update a subject")
    @PutMapping("/subjects/{id}")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return subjectService.update(id, request);
    }

    @Operation(summary = "Delete a subject")
    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}