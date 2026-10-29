package com.ipt.siga.controllers;

import com.ipt.siga.dtos.StudentCreateRequest;
import com.ipt.siga.dtos.StudentResponse;
import com.ipt.siga.dtos.StudentUpdateRequest;
import com.ipt.siga.enums.StudentStatus;
import com.ipt.siga.services.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Tag(name = "Students", description = "Management of students")
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "Register a student")
    @PostMapping
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.create(request));
    }

    @Operation(summary = "List students (optional filters)")
    @GetMapping
    public List<StudentResponse> findAll(@RequestParam(required = false) Long careerId,
                                         @RequestParam(required = false) StudentStatus status) {
        return studentService.findAll(careerId, status);
    }

    @Operation(summary = "Get a student by id")
    @GetMapping("/{id}")
    public StudentResponse findById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @Operation(summary = "Update a student, including its status")
    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return studentService.update(id, request);
    }

    @Operation(summary = "Delete a student")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}