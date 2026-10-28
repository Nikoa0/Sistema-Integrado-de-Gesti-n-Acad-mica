package com.ipt.siga.controllers;

import com.ipt.siga.dtos.EnrollmentResponse;
import com.ipt.siga.dtos.GradeRequest;
import com.ipt.siga.services.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Grades", description = "Registration of final grades")
public class GradeController {

    private final GradeService gradeService;

    @Operation(summary = "Register or modify the final grade of an enrollment (responsible teacher only)")
    @PutMapping("/enrollments/{enrollmentId}/grade")
    public EnrollmentResponse register(@PathVariable Long enrollmentId,
                                       @Valid @RequestBody GradeRequest request) {
        return gradeService.register(enrollmentId, request);
    }
}