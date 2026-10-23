package com.ipt.siga.controllers;

import com.ipt.siga.dtos.SubjectRequest;
import com.ipt.siga.dtos.SubjectResponse;
import com.ipt.siga.services.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping("/study-plans/{planId}/subjects")
    public ResponseEntity<SubjectResponse> create(@PathVariable Long planId,
                                                  @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.create(planId, request));
    }

    @GetMapping("/study-plans/{planId}/subjects")
    public List<SubjectResponse> findByPlan(@PathVariable Long planId,
                                            @RequestParam(required = false) Integer semester) {
        return subjectService.findByPlan(planId, semester);
    }

    @GetMapping("/subjects/{id}")
    public SubjectResponse findById(@PathVariable Long id) {
        return subjectService.findById(id);
    }

    @PutMapping("/subjects/{id}")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return subjectService.update(id, request);
    }

    @DeleteMapping("/subjects/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}