package com.ipt.siga.controllers;

import com.ipt.siga.dtos.EnrollmentRequest;
import com.ipt.siga.dtos.EnrollmentResponse;
import com.ipt.siga.dtos.SectionResponse;
import com.ipt.siga.services.EnrollmentService;
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
@Tag(name = "Enrollments", description = "Academic enrollment of students")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Operation(summary = "Offer available to a student in an academic period, according to the student's study plan")
    @GetMapping("/students/{studentId}/offer")
    public List<SectionResponse> findOffer(@PathVariable Long studentId, @RequestParam Long periodId) {
        return enrollmentService.findOffer(studentId, periodId);
    }

    @Operation(summary = "Enroll a list of subjects for a student in an academic period")
    @PostMapping("/students/{studentId}/enrollments")
    public ResponseEntity<List<EnrollmentResponse>> enroll(@PathVariable Long studentId,
                                                           @Valid @RequestBody EnrollmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(studentId, request));
    }

    @Operation(summary = "List the enrollments of a student (optional period filter)")
    @GetMapping("/students/{studentId}/enrollments")
    public List<EnrollmentResponse> findByStudent(@PathVariable Long studentId,
                                                  @RequestParam(required = false) Long periodId) {
        return enrollmentService.findByStudent(studentId, periodId);
    }

    @Operation(summary = "List the enrollments of an academic period")
    @GetMapping("/academic-periods/{periodId}/enrollments")
    public List<EnrollmentResponse> findByPeriod(@PathVariable Long periodId) {
        return enrollmentService.findByPeriod(periodId);
    }

    @Operation(summary = "List the students enrolled in a section")
    @GetMapping("/sections/{sectionId}/enrollments")
    public List<EnrollmentResponse> findBySection(@PathVariable Long sectionId) {
        return enrollmentService.findBySection(sectionId);
    }

    @Operation(summary = "Cancel an enrollment")
    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        enrollmentService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}