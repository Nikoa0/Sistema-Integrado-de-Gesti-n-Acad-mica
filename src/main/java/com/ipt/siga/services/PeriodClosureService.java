package com.ipt.siga.services;

import com.ipt.siga.dtos.ClosureResponse;
import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Enrollment;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.enums.StudentStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.repositories.AcademicPeriodRepository;
import com.ipt.siga.repositories.EnrollmentRepository;
import com.ipt.siga.repositories.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeriodClosureService {

    private static final int MAX_FAILURES = 2;

    private final AcademicPeriodService academicPeriodService;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;

    @Transactional
    public ClosureResponse close(Long periodId) {
        AcademicPeriod period = academicPeriodService.getPeriodOrThrow(periodId);
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("Only an OPEN academic period can be closed");
        }

        // 1. Every enrollment must have a grade
        List<Enrollment> pending = enrollmentRepository.findByPeriodIdAndGradeIsNull(periodId);
        if (!pending.isEmpty()) {
            Map<String, Long> bySubject = pending.stream().collect(Collectors.groupingBy(
                    e -> e.getSubject().getCode(), TreeMap::new, Collectors.counting()));
            throw new BusinessRuleException(
                    "The period cannot be closed. Sections with missing grades (subject=pending): " + bySubject);
        }

        // 2. Close first, so the period's grades count as academic history in the queries below
        period.setStatus(PeriodStatus.CLOSED);
        academicPeriodRepository.saveAndFlush(period);

        // 3. Update the status of every student who had enrollments in the period
        Map<Long, List<Enrollment>> byStudent = enrollmentRepository.findByPeriodId(periodId).stream()
                .collect(Collectors.groupingBy(e -> e.getStudent().getId(), LinkedHashMap::new, Collectors.toList()));

        List<String> graduated = new ArrayList<>();
        List<String> eliminated = new ArrayList<>();
        for (List<Enrollment> enrollments : byStudent.values()) {
            Student student = enrollments.get(0).getStudent();
            if (hasFailedTwice(student, enrollments)) {
                student.setStatus(StudentStatus.ELIMINATED);
                eliminated.add(student.getRun());
            } else if (hasApprovedEverything(student)) {
                student.setStatus(StudentStatus.GRADUATED);
                graduated.add(student.getRun());
            }
        }

        return new ClosureResponse(period.getId(),
                period.getAcademicYear() + "-" + period.getSemester(),
                period.getStatus(), graduated, eliminated);
    }

    private boolean hasFailedTwice(Student student, List<Enrollment> enrollmentsOfPeriod) {
        return enrollmentsOfPeriod.stream()
                .filter(e -> e.getGrade() < EnrollmentService.PASSING_GRADE)
                .anyMatch(e -> enrollmentRepository.countFailures(student.getId(), e.getSubject().getId(),
                        PeriodStatus.CLOSED, EnrollmentService.PASSING_GRADE) >= MAX_FAILURES);
    }

    private boolean hasApprovedEverything(Student student) {
        Set<Long> approved = new HashSet<>(enrollmentRepository.findApprovedSubjectIds(
                student.getId(), PeriodStatus.CLOSED, EnrollmentService.PASSING_GRADE));
        return subjectRepository.findByStudyPlanId(student.getStudyPlan().getId()).stream()
                .map(Subject::getId)
                .allMatch(approved::contains);
    }
}