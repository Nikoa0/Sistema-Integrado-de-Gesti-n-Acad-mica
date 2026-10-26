package com.ipt.siga.services;

import com.ipt.siga.dtos.AcademicPeriodRequest;
import com.ipt.siga.dtos.AcademicPeriodResponse;
import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.AcademicPeriodRepository;
import com.ipt.siga.repositories.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ipt.siga.repositories.MatriculationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final SectionRepository sectionRepository;
    private final MatriculationRepository matriculationRepository;

    @Transactional
    public AcademicPeriodResponse create(AcademicPeriodRequest request) {
        if (academicPeriodRepository.existsByAcademicYearAndSemester(request.year(), request.semester())) {
            throw new BusinessRuleException(
                    "Academic period " + request.year() + "-" + request.semester() + " already exists");
        }
        AcademicPeriod period = new AcademicPeriod();
        period.setAcademicYear(request.year());
        period.setSemester(request.semester());
        period.setStatus(PeriodStatus.OPEN);
        return toResponse(academicPeriodRepository.save(period));
    }

    @Transactional(readOnly = true)
    public List<AcademicPeriodResponse> findAll() {
        return academicPeriodRepository.findAllByOrderByAcademicYearDescSemesterDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AcademicPeriodResponse findById(Long id) {
        return toResponse(getPeriodOrThrow(id));
    }

    @Transactional
    public void delete(Long id) {
        AcademicPeriod period = getPeriodOrThrow(id);
        if (matriculationRepository.existsByPeriodId(id)) {
            throw new BusinessRuleException("Academic period has matriculations and cannot be deleted");
        }
        if (sectionRepository.existsByPeriodId(id)) {
            throw new BusinessRuleException("Academic period has sections and cannot be deleted");
        }
        academicPeriodRepository.delete(period);
    }

    public AcademicPeriod getPeriodOrThrow(Long id) {
        return academicPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period not found with id " + id));
    }

    private AcademicPeriodResponse toResponse(AcademicPeriod p) {
        return new AcademicPeriodResponse(p.getId(), p.getAcademicYear(), p.getSemester(),
                p.getAcademicYear() + "-" + p.getSemester(), p.getStatus());
    }
}