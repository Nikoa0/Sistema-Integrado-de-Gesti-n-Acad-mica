package com.ipt.siga.services;

import com.ipt.siga.dtos.CareerRequest;
import com.ipt.siga.dtos.CareerResponse;
import com.ipt.siga.entities.Career;
import com.ipt.siga.enums.CareerStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.CareerRepository;
import com.ipt.siga.repositories.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CareerService {

    private static final int DURATION_SEMESTERS = 4;

    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;

    @Transactional
    public CareerResponse create(CareerRequest request) {
        if (careerRepository.existsByCode(request.code())) {
            throw new BusinessRuleException("A career with code " + request.code() + " already exists");
        }
        Career career = new Career();
        career.setCode(request.code());
        career.setName(request.name());
        career.setDescription(request.description());
        career.setStatus(request.status() != null ? request.status() : CareerStatus.ACTIVE);
        career.setDurationSemesters(DURATION_SEMESTERS);
        return toResponse(careerRepository.save(career));
    }

    @Transactional(readOnly = true)
    public List<CareerResponse> findAll() {
        return careerRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CareerResponse findById(Long id) {
        return toResponse(getCareerOrThrow(id));
    }

    @Transactional
    public CareerResponse update(Long id, CareerRequest request) {
        Career career = getCareerOrThrow(id);
        if (careerRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new BusinessRuleException("A career with code " + request.code() + " already exists");
        }
        career.setCode(request.code());
        career.setName(request.name());
        career.setDescription(request.description());
        if (request.status() != null) {
            career.setStatus(request.status());
        }
        return toResponse(careerRepository.save(career));
    }

    @Transactional
    public void delete(Long id) {
        Career career = getCareerOrThrow(id);
        // TODO (Epic 1): also reject if the career has students
        if (studyPlanRepository.existsByCareerId(id)) {
            throw new BusinessRuleException(
                    "Career has study plans and cannot be deleted. Change its status to INACTIVE instead");
        }
        careerRepository.delete(career);
    }

    public Career getCareerOrThrow(Long id) {
        return careerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Career not found with id " + id));
    }

    private CareerResponse toResponse(Career c) {
        return new CareerResponse(c.getId(), c.getCode(), c.getName(),
                c.getDescription(), c.getStatus(), c.getDurationSemesters());
    }
}