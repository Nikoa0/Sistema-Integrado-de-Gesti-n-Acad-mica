package com.ipt.siga.controllers;

import com.ipt.siga.dtos.StudentCreateRequest;
import com.ipt.siga.dtos.StudentResponse;
import com.ipt.siga.dtos.StudentStatusRequest;
import com.ipt.siga.dtos.StudentUpdateRequest;
import com.ipt.siga.enums.StudentStatus;
import com.ipt.siga.services.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(request));
    }

    @GetMapping
    public List<StudentResponse> findAll(@RequestParam(required = false) Long careerId,
                                         @RequestParam(required = false) StudentStatus status) {
        return studentService.findAll(careerId, status);
    }

    @GetMapping("/{id}")
    public StudentResponse findById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return studentService.update(id, request);
    }

    @PutMapping("/{id}/status")
    public StudentResponse updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody StudentStatusRequest request) {
        return studentService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}