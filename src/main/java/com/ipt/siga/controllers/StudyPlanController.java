package com.ipt.siga.controllers;

import com.ipt.siga.dtos.StudyPlanRequest;
import com.ipt.siga.dtos.StudyPlanResponse;
import com.ipt.siga.services.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @PostMapping("/careers/{careerId}/study-plans")
    public ResponseEntity<StudyPlanResponse> create(@PathVariable Long careerId,
                                                    @Valid @RequestBody StudyPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studyPlanService.create(careerId, request));
    }

    @GetMapping("/careers/{careerId}/study-plans")
    public List<StudyPlanResponse> findByCareer(@PathVariable Long careerId) {
        return studyPlanService.findByCareer(careerId);
    }

    @GetMapping("/study-plans/{id}")
    public StudyPlanResponse findById(@PathVariable Long id) {
        return studyPlanService.findById(id);
    }

    @PutMapping("/study-plans/{id}")
    public StudyPlanResponse update(@PathVariable Long id, @Valid @RequestBody StudyPlanRequest request) {
        return studyPlanService.update(id, request);
    }

    @DeleteMapping("/study-plans/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studyPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}