package com.ipt.siga.services;

import com.ipt.siga.dtos.SubjectRequest;
import com.ipt.siga.dtos.SubjectResponse;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ipt.siga.repositories.SectionRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private static final int MAX_PREREQUISITES = 3;
    private static final int WEEKLY_HOURS_LIMIT = 8; // total must be LOWER than this

    private final SubjectRepository subjectRepository;
    private final StudyPlanService studyPlanService;
    private final SectionRepository sectionRepository;

    @Transactional
    public SubjectResponse create(Long planId, SubjectRequest request) {
        StudyPlan plan = studyPlanService.getPlanOrThrow(planId);

        if (subjectRepository.existsByStudyPlanIdAndCode(planId, request.code())) {
            throw new BusinessRuleException(
                    "Subject code " + request.code() + " already exists in this study plan");
        }
        validateWeeklyHours(request);

        Subject subject = new Subject();
        subject.setStudyPlan(plan);
        applyFields(subject, request);
        subject.setPrerequisites(resolvePrerequisites(planId, request));
        return toResponse(subjectRepository.save(subject));
    }

    @Transactional(readOnly = true)
    public List<SubjectResponse> findByPlan(Long planId, Integer semester) {
        studyPlanService.getPlanOrThrow(planId);
        List<Subject> subjects = (semester == null)
                ? subjectRepository.findByStudyPlanId(planId)
                : subjectRepository.findByStudyPlanIdAndSemester(planId, semester);
        return subjects.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SubjectResponse findById(Long id) {
        return toResponse(getSubjectOrThrow(id));
    }

    @Transactional
    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = getSubjectOrThrow(id);
        Long planId = subject.getStudyPlan().getId();

        if (subjectRepository.existsByStudyPlanIdAndCodeAndIdNot(planId, request.code(), id)) {
            throw new BusinessRuleException(
                    "Subject code " + request.code() + " already exists in this study plan");
        }
        validateWeeklyHours(request);

        // Subjects that require this one must stay in a later semester
        for (Subject dependent : subjectRepository.findDependents(id)) {
            if (dependent.getSemester() <= request.semester()) {
                throw new BusinessRuleException("Subject " + dependent.getCode()
                        + " requires this subject, so it must stay in an earlier semester");
            }
        }

        applyFields(subject, request);
        subject.setPrerequisites(resolvePrerequisites(planId, request));
        return toResponse(subjectRepository.save(subject));
    }

    @Transactional
    public void delete(Long id) {
        Subject subject = getSubjectOrThrow(id);
        // TODO (Epic 7): reject if the subject is part of any student's academic history
        if (sectionRepository.existsBySubjectId(id)) {
            throw new BusinessRuleException("Subject has sections and cannot be deleted");
        }
        if (!subjectRepository.findDependents(id).isEmpty()) {
            throw new BusinessRuleException(
                    "Subject is a prerequisite of other subjects and cannot be deleted");
        }
        subjectRepository.delete(subject);
    }

    public Subject getSubjectOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id " + id));
    }

    private void validateWeeklyHours(SubjectRequest r) {
        int total = r.theoryHours() + r.exerciseHours() + r.labHours();
        if (total >= WEEKLY_HOURS_LIMIT) {
            throw new BusinessRuleException(
                    "Total weekly hours must be lower than " + WEEKLY_HOURS_LIMIT + " (received " + total + ")");
        }
    }

    private Set<Subject> resolvePrerequisites(Long planId, SubjectRequest request) {
        Set<String> codes = request.prerequisiteCodes();
        if (codes == null || codes.isEmpty()) {
            return new HashSet<>();
        }
        if (codes.size() > MAX_PREREQUISITES) {
            throw new BusinessRuleException(
                    "A subject can have at most " + MAX_PREREQUISITES + " prerequisites");
        }

        List<Subject> found = subjectRepository.findByStudyPlanIdAndCodeIn(planId, codes);
        if (found.size() != codes.size()) {
            Set<String> foundCodes = found.stream().map(Subject::getCode).collect(Collectors.toSet());
            Set<String> missing = new HashSet<>(codes);
            missing.removeAll(foundCodes);
            throw new BusinessRuleException(
                    "Prerequisites not found in this study plan: " + missing);
        }

        for (Subject prerequisite : found) {
            if (prerequisite.getSemester() >= request.semester()) {
                throw new BusinessRuleException("Prerequisite " + prerequisite.getCode()
                        + " must belong to an earlier semester");
            }
        }
        return new HashSet<>(found);
    }

    private void applyFields(Subject subject, SubjectRequest request) {
        subject.setCode(request.code());
        subject.setName(request.name());
        subject.setSemester(request.semester());
        subject.setTheoryHours(request.theoryHours());
        subject.setExerciseHours(request.exerciseHours());
        subject.setLabHours(request.labHours());
        subject.setSct(request.sct());
    }

    private SubjectResponse toResponse(Subject s) {
        List<String> prerequisiteCodes = s.getPrerequisites().stream()
                .map(Subject::getCode)
                .sorted()
                .toList();
        return new SubjectResponse(s.getId(), s.getStudyPlan().getId(), s.getCode(), s.getName(),
                s.getSemester(), s.getTheoryHours(), s.getExerciseHours(), s.getLabHours(),
                s.getSct(), prerequisiteCodes);
    }
}