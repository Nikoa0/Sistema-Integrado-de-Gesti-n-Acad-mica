package com.ipt.siga.controllers;

import com.ipt.siga.dtos.TeacherCreateRequest;
import com.ipt.siga.dtos.TeacherResponse;
import com.ipt.siga.dtos.TeacherUpdateRequest;
import com.ipt.siga.enums.TeacherStatus;
import com.ipt.siga.services.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
@Tag(name = "Teachers", description = "Management of teachers")
public class TeacherController {

    private final TeacherService teacherService;

    @Operation(summary = "Register a teacher")
    @PostMapping
    public ResponseEntity<TeacherResponse> create(@Valid @RequestBody TeacherCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.create(request));
    }

    @Operation(summary = "List teachers (optional status filter)")
    @GetMapping
    public List<TeacherResponse> findAll(@RequestParam(required = false) TeacherStatus status) {
        return teacherService.findAll(status);
    }

    @Operation(summary = "Get a teacher by id")
    @GetMapping("/{id}")
    public TeacherResponse findById(@PathVariable Long id) {
        return teacherService.findById(id);
    }

    @Operation(summary = "Update a teacher, including its status")
    @PutMapping("/{id}")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody TeacherUpdateRequest request) {
        return teacherService.update(id, request);
    }

    @Operation(summary = "Delete a teacher")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}