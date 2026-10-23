package com.ipt.siga.services;

import com.ipt.siga.dtos.StudyPlanRequest;
import com.ipt.siga.dtos.StudyPlanResponse;
import com.ipt.siga.entities.Career;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.enums.StudyPlanStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.StudyPlanRepository;
import com.ipt.siga.repositories.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final CareerService careerService;
    private final SubjectRepository subjectRepository;

    @Transactional
    public StudyPlanResponse create(Long careerId, StudyPlanRequest request) {
        Career career = careerService.getCareerOrThrow(careerId);

        if (studyPlanRepository.existsByCareerIdAndIdentifier(careerId, request.identifier())) {
            throw new BusinessRuleException(
                    "Identifier " + request.identifier() + " already exists for this career");
        }

        boolean makeCurrent = Boolean.TRUE.equals(request.current());
        if (makeCurrent) {
            demoteCurrentPlan(careerId);
        }

        StudyPlan plan = new StudyPlan();
        plan.setCareer(career);
        plan.setIdentifier(request.identifier());
        plan.setStatus(makeCurrent ? StudyPlanStatus.CURRENT : StudyPlanStatus.NOT_CURRENT);
        return toResponse(studyPlanRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public List<StudyPlanResponse> findByCareer(Long careerId) {
        careerService.getCareerOrThrow(careerId);
        return studyPlanRepository.findByCareerId(careerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StudyPlanResponse findById(Long id) {
        return toResponse(getPlanOrThrow(id));
    }

    @Transactional
    public StudyPlanResponse update(Long id, StudyPlanRequest request) {
        StudyPlan plan = getPlanOrThrow(id);
        Long careerId = plan.getCareer().getId();

        if (studyPlanRepository.existsByCareerIdAndIdentifierAndIdNot(careerId, request.identifier(), id)) {
            throw new BusinessRuleException(
                    "Identifier " + request.identifier() + " already exists for this career");
        }
        plan.setIdentifier(request.identifier());

        if (request.current() != null) {
            if (request.current() && plan.getStatus() != StudyPlanStatus.CURRENT) {
                demoteCurrentPlan(careerId);
                plan.setStatus(StudyPlanStatus.CURRENT);
            } else if (!request.current()) {
                plan.setStatus(StudyPlanStatus.NOT_CURRENT);
            }
        }
        return toResponse(studyPlanRepository.save(plan));
    }

    @Transactional
    public void delete(Long id) {
        StudyPlan plan = getPlanOrThrow(id);
        // TODO (Epic 1): reject if the plan has students
        if (subjectRepository.existsByStudyPlanId(id)) {
            throw new BusinessRuleException("Study plan has subjects and cannot be deleted");
        }
        studyPlanRepository.delete(plan);
    }

    private void demoteCurrentPlan(Long careerId) {
        studyPlanRepository.findByCareerIdAndStatus(careerId, StudyPlanStatus.CURRENT)
                .ifPresent(old -> {
                    old.setStatus(StudyPlanStatus.NOT_CURRENT);
                    studyPlanRepository.saveAndFlush(old);
                });
    }

    public StudyPlan getPlanOrThrow(Long id) {
        return studyPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan not found with id " + id));
    }

    private StudyPlanResponse toResponse(StudyPlan p) {
        return new StudyPlanResponse(p.getId(), p.getCareer().getId(), p.getIdentifier(), p.getStatus());
    }
}