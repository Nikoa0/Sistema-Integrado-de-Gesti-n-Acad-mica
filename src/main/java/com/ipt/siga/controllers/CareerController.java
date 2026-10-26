package com.ipt.siga.controllers;

import com.ipt.siga.dtos.CareerRequest;
import com.ipt.siga.dtos.CareerResponse;
import com.ipt.siga.services.CareerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
@RequiredArgsConstructor
@Tag(name = "Careers", description = "Management of careers")
public class CareerController {

    private final CareerService careerService;

    @Operation(summary = "Create a career")
    @ApiResponse(responseCode = "201", description = "Career created")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "409", description = "Career code already exists")
    @PostMapping
    public ResponseEntity<CareerResponse> create(@Valid @RequestBody CareerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(careerService.create(request));
    }

    @Operation(summary = "List all careers")
    @GetMapping
    public List<CareerResponse> findAll() {
        return careerService.findAll();
    }

    @Operation(summary = "Get a career by id")
    @ApiResponse(responseCode = "404", description = "Career not found")
    @GetMapping("/{id}")
    public CareerResponse findById(@PathVariable Long id) {
        return careerService.findById(id);
    }

    @Operation(summary = "Update a career, including its status")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "404", description = "Career not found")
    @ApiResponse(responseCode = "409", description = "Career code already exists")
    @PutMapping("/{id}")
    public CareerResponse update(@PathVariable Long id, @Valid @RequestBody CareerRequest request) {
        return careerService.update(id, request);
    }

    @Operation(summary = "Delete a career (only if it has no study plans or students)")
    @ApiResponse(responseCode = "204", description = "Career deleted")
    @ApiResponse(responseCode = "404", description = "Career not found")
    @ApiResponse(responseCode = "409", description = "Career has study plans or students")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        careerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}