package com.ipt.siga.controllers;

import com.ipt.siga.dtos.TeacherCreateRequest;
import com.ipt.siga.dtos.TeacherResponse;
import com.ipt.siga.dtos.TeacherStatusRequest;
import com.ipt.siga.dtos.TeacherUpdateRequest;
import com.ipt.siga.enums.TeacherStatus;
import com.ipt.siga.services.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.create(request));
    }

    @GetMapping
    public List<TeacherResponse> findAll(@RequestParam(required = false) TeacherStatus status) {
        return teacherService.findAll(status);
    }

    @GetMapping("/{id}")
    public TeacherResponse findById(@PathVariable Long id) {
        return teacherService.findById(id);
    }

    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherUpdateRequest request) {
        return teacherService.update(id, request);
    }

    @PutMapping("/{id}/status")
    public TeacherResponse updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody TeacherStatusRequest request) {
        return teacherService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}