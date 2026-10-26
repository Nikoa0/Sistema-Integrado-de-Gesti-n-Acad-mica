package com.ipt.siga.services;

import com.ipt.siga.dtos.EnrollmentRequest;
import com.ipt.siga.dtos.EnrollmentResponse;
import com.ipt.siga.dtos.SectionResponse;
import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Enrollment;
import com.ipt.siga.entities.ScheduleBlock;
import com.ipt.siga.entities.Section;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.enums.StudentStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.EnrollmentRepository;
import com.ipt.siga.repositories.MatriculationRepository;
import com.ipt.siga.repositories.SectionRepository;
import com.ipt.siga.repositories.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final int MIN_SUBJECTS = 3;
    private static final int MAX_SUBJECTS = 6;
    public static final double PASSING_GRADE = 4.0;

    private final EnrollmentRepository enrollmentRepository;
    private final MatriculationRepository matriculationRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final StudentService studentService;
    private final AcademicPeriodService academicPeriodService;
    private final SectionService sectionService;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<EnrollmentResponse> enroll(Long studentId, EnrollmentRequest request) {
        Student student = studentService.getStudentOrThrow(studentId);
        AcademicPeriod period = academicPeriodService.getPeriodOrThrow(request.periodId());
        requireOpen(period);

        // Student must be REGULAR and matriculated in the period
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new BusinessRuleException("Only REGULAR students can enroll subjects");
        }
        if (!matriculationRepository.existsByStudentIdAndPeriodId(studentId, period.getId())) {
            throw new BusinessRuleException("The student is not matriculated in this academic period");
        }

        Set<Long> requestedIds = new HashSet<>(request.subjectIds());
        if (requestedIds.size() != request.subjectIds().size()) {
            throw new BusinessRuleException("The request contains repeated subjects");
        }

        // Subjects must exist and belong to the student's study plan
        List<Subject> selected = subjectRepository.findAllById(requestedIds);
        if (selected.size() != requestedIds.size()) {
            throw new ResourceNotFoundException("One or more subjects do not exist");
        }
        Long planId = student.getStudyPlan().getId();
        for (Subject subject : selected) {
            if (!subject.getStudyPlan().getId().equals(planId)) {
                throw new BusinessRuleException(
                        "Subject " + subject.getCode() + " does not belong to the student's study plan");
            }
        }

        // Subjects must be offered in the period (this also locks the sections)
        List<Section> sections = sectionRepository
                .findByPeriodIdAndSubjectIdInOrderById(period.getId(), requestedIds);
        Map<Long, Section> sectionBySubject = sections.stream()
                .collect(Collectors.toMap(s -> s.getSubject().getId(), Function.identity()));
        for (Subject subject : selected) {
            if (!sectionBySubject.containsKey(subject.getId())) {
                throw new BusinessRuleException(
                        "Subject " + subject.getCode() + " is not offered in this academic period");
            }
        }

        // Not enrolled twice in the same period
        List<Enrollment> existing = enrollmentRepository.findByStudentIdAndPeriodId(studentId, period.getId());
        Set<Long> enrolledSubjectIds = existing.stream()
                .map(e -> e.getSubject().getId()).collect(Collectors.toSet());
        for (Subject subject : selected) {
            if (enrolledSubjectIds.contains(subject.getId())) {
                throw new BusinessRuleException(
                        "Subject " + subject.getCode() + " is already enrolled in this academic period");
            }
        }

        // Approved subjects cannot be enrolled again, and prerequisites must be approved
        Set<Long> approved = new HashSet<>(enrollmentRepository
                .findApprovedSubjectIds(studentId, PeriodStatus.CLOSED, PASSING_GRADE));
        for (Subject subject : selected) {
            if (approved.contains(subject.getId())) {
                throw new BusinessRuleException("Subject " + subject.getCode() + " is already approved");
            }
            List<String> missing = subject.getPrerequisites().stream()
                    .filter(p -> !approved.contains(p.getId()))
                    .map(Subject::getCode).sorted().toList();
            if (!missing.isEmpty()) {
                throw new BusinessRuleException("Subject " + subject.getCode()
                        + " requires approved prerequisites: " + missing);
            }
        }

        // Minimum 3 and maximum 6 subjects per period
        int finalCount = existing.size() + selected.size();
        if (finalCount > MAX_SUBJECTS) {
            throw new BusinessRuleException("A student can enroll at most " + MAX_SUBJECTS
                    + " subjects per academic period (would have " + finalCount + ")");
        }

        List<Subject> planSubjects = subjectRepository.findByStudyPlanId(planId);
        Set<Long> offeredSubjectIds = sectionRepository.findByPeriodId(period.getId()).stream()
                .map(s -> s.getSubject().getId()).collect(Collectors.toSet());
        List<Subject> pending = planSubjects.stream()
                .filter(s -> !approved.contains(s.getId())).toList();
        List<Subject> available = pending.stream()
                .filter(s -> offeredSubjectIds.contains(s.getId()))
                .filter(s -> s.getPrerequisites().stream().allMatch(p -> approved.contains(p.getId())))
                .toList();

        // With fewer than 3 subjects left in the plan, the pending available ones can be enrolled
        int minimum = pending.size() >= MIN_SUBJECTS ? MIN_SUBJECTS : available.size();
        if (finalCount < minimum) {
            throw new BusinessRuleException("A student must enroll at least " + minimum
                    + " subjects per academic period (would have " + finalCount + ")");
        }

        // No higher-semester subjects while lower-semester ones are pending and available
        Set<Long> finalIds = new HashSet<>(enrolledSubjectIds);
        finalIds.addAll(requestedIds);
        int highestSemester = 0;
        for (Enrollment e : existing) {
            highestSemester = Math.max(highestSemester, e.getSubject().getSemester());
        }
        for (Subject subject : selected) {
            highestSemester = Math.max(highestSemester, subject.getSemester());
        }
        for (Subject candidate : available) {
            if (!finalIds.contains(candidate.getId()) && candidate.getSemester() < highestSemester) {
                throw new BusinessRuleException("Subject " + candidate.getCode() + " (semester "
                        + candidate.getSemester() + ") is pending and available, so subjects of a higher semester cannot be enrolled");
            }
        }

        // Section capacity
        for (Section section : sections) {
            if (enrollmentRepository.countBySectionId(section.getId()) >= section.getCapacity()) {
                throw new BusinessRuleException(
                        "The section of " + section.getSubject().getCode() + " has no seats available");
            }
        }

        // No schedule conflicts between the student's sections
        List<Section> allSections = new ArrayList<>(existing.stream().map(Enrollment::getSection).toList());
        allSections.addAll(sections);
        Map<ScheduleBlock, Section> usedBlocks = new HashMap<>();
        for (Section section : allSections) {
            for (ScheduleBlock block : section.getBlocks()) {
                Section other = usedBlocks.put(block, section);
                if (other != null) {
                    throw new BusinessRuleException("Schedule conflict at block " + block.toCode()
                            + " between " + other.getSubject().getCode()
                            + " and " + section.getSubject().getCode());
                }
            }
        }

        // Everything is valid: register the enrollments
        List<Enrollment> created = new ArrayList<>();
        for (Subject subject : selected) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setSubject(subject);
            enrollment.setSection(sectionBySubject.get(subject.getId()));
            enrollment.setPeriod(period);
            enrollment.setEnrolledAt(LocalDate.now());
            created.add(enrollment);
        }
        return enrollmentRepository.saveAll(created).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void cancel(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id " + enrollmentId));
        requireOpen(enrollment.getPeriod());
        enrollmentRepository.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> findOffer(Long studentId, Long periodId) {
        Student student = studentService.getStudentOrThrow(studentId);
        academicPeriodService.getPeriodOrThrow(periodId);
        return sectionService.findOffer(periodId, student.getStudyPlan().getId());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findByStudent(Long studentId, Long periodId) {
        studentService.getStudentOrThrow(studentId);
        List<Enrollment> enrollments = (periodId == null)
                ? enrollmentRepository.findByStudentId(studentId)
                : enrollmentRepository.findByStudentIdAndPeriodId(studentId, periodId);
        return enrollments.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findByPeriod(Long periodId) {
        academicPeriodService.getPeriodOrThrow(periodId);
        return enrollmentRepository.findByPeriodId(periodId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findBySection(Long sectionId) {
        sectionService.getSectionOrThrow(sectionId);
        return enrollmentRepository.findBySectionId(sectionId).stream().map(this::toResponse).toList();
    }

    private void requireOpen(AcademicPeriod period) {
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("The academic period is CLOSED: enrollments and cancellations are not allowed");
        }
    }

    private EnrollmentResponse toResponse(Enrollment e) {
        return new EnrollmentResponse(e.getId(),
                e.getStudent().getId(), e.getStudent().getFullName(),
                e.getSubject().getId(), e.getSubject().getCode(), e.getSubject().getName(),
                e.getSection().getId(),
                e.getPeriod().getId(), e.getPeriod().getAcademicYear() + "-" + e.getPeriod().getSemester(),
                e.getEnrolledAt(), e.getGrade());
    }
}