package com.ipt.siga.controllers;

import com.ipt.siga.dtos.CareerRequest;
import com.ipt.siga.dtos.CareerResponse;
import com.ipt.siga.services.CareerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/careers")
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;

    @PostMapping
    public ResponseEntity<CareerResponse> create(@Valid @RequestBody CareerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(careerService.create(request));
    }

    @GetMapping
    public List<CareerResponse> findAll() {
        return careerService.findAll();
    }

    @GetMapping("/{id}")
    public CareerResponse findById(@PathVariable Long id) {
        return careerService.findById(id);
    }

    @PutMapping("/{id}")
    public CareerResponse update(@PathVariable Long id, @Valid @RequestBody CareerRequest request) {
        return careerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        careerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}