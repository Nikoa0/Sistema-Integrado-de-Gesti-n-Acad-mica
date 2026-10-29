package com.ipt.siga.services;

import com.ipt.siga.dtos.ClosureResponse;
import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Enrollment;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.enums.StudentStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.repositories.AcademicPeriodRepository;
import com.ipt.siga.repositories.EnrollmentRepository;
import com.ipt.siga.repositories.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeriodClosureServiceTest {

    @Mock
    private AcademicPeriodService academicPeriodService;
    @Mock
    private AcademicPeriodRepository academicPeriodRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private PeriodClosureService periodClosureService;

    private AcademicPeriod period;
    private Student student;
    private Subject subject1;
    private Subject subject2;

    @BeforeEach
    void setUp() {
        period = new AcademicPeriod();
        period.setId(1L);
        period.setAcademicYear(2028);
        period.setSemester(1);
        period.setStatus(PeriodStatus.OPEN);

        StudyPlan plan = new StudyPlan();
        plan.setId(10L);

        student = new Student();
        student.setId(5L);
        student.setRun("12345678-K");
        student.setStatus(StudentStatus.REGULAR);
        student.setStudyPlan(plan);

        subject1 = subject(1L, "TAP101");
        subject2 = subject(2L, "TAP102");
    }

    private Subject subject(Long id, String code) {
        Subject s = new Subject();
        s.setId(id);
        s.setCode(code);
        return s;
    }

    private Enrollment enrollment(Subject subject, Double grade) {
        Enrollment e = new Enrollment();
        e.setStudent(student);
        e.setSubject(subject);
        e.setGrade(grade);
        return e;
    }

    // ---------- rejections ----------

    @Test
    void close_periodAlreadyClosed_throws() {
        period.setStatus(PeriodStatus.CLOSED);
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period);

        assertThrows(BusinessRuleException.class, () -> periodClosureService.close(1L));

        verify(academicPeriodRepository, never()).saveAndFlush(any());
    }

    @Test
    void close_enrollmentsWithoutGrade_throwsAndNamesTheSubjects() {
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period);
        when(enrollmentRepository.findByPeriodIdAndGradeIsNull(1L))
                .thenReturn(List.of(enrollment(subject1, null), enrollment(subject1, null)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> periodClosureService.close(1L));

        assertTrue(ex.getMessage().contains("TAP101"));
        assertEquals(PeriodStatus.OPEN, period.getStatus());
        verify(academicPeriodRepository, never()).saveAndFlush(any());
    }

    // ---------- successful closures ----------

    @Test
    void close_studentWithPendingSubjects_staysRegular() {
        Enrollment e = enrollment(subject1, 5.0);
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period);
        when(enrollmentRepository.findByPeriodIdAndGradeIsNull(1L)).thenReturn(List.of());
        when(enrollmentRepository.findByPeriodId(1L)).thenReturn(List.of(e));
        when(enrollmentRepository.findApprovedSubjectIds(5L, PeriodStatus.CLOSED, 4.0))
                .thenReturn(List.of(1L));
        when(subjectRepository.findByStudyPlanId(10L)).thenReturn(List.of(subject1, subject2));

        ClosureResponse response = periodClosureService.close(1L);

        assertEquals(PeriodStatus.CLOSED, period.getStatus());
        assertEquals("2028-1", response.periodName());
        assertTrue(response.graduatedRuns().isEmpty());
        assertTrue(response.eliminatedRuns().isEmpty());
        assertEquals(StudentStatus.REGULAR, student.getStatus());
        verify(academicPeriodRepository).saveAndFlush(period);
    }

    @Test
    void close_studentFailsSameSubjectTwice_isEliminated() {
        Enrollment e = enrollment(subject1, 3.0);
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period);
        when(enrollmentRepository.findByPeriodIdAndGradeIsNull(1L)).thenReturn(List.of());
        when(enrollmentRepository.findByPeriodId(1L)).thenReturn(List.of(e));
        when(enrollmentRepository.countFailures(5L, 1L, PeriodStatus.CLOSED, 4.0)).thenReturn(2L);

        ClosureResponse response = periodClosureService.close(1L);

        assertEquals(StudentStatus.ELIMINATED, student.getStatus());
        assertEquals(List.of("12345678-K"), response.eliminatedRuns());
        assertTrue(response.graduatedRuns().isEmpty());
    }

    @Test
    void close_studentFailsOnlyOnce_isNotEliminated() {
        Enrollment e = enrollment(subject1, 3.0);
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period);
        when(enrollmentRepository.findByPeriodIdAndGradeIsNull(1L)).thenReturn(List.of());
        when(enrollmentRepository.findByPeriodId(1L)).thenReturn(List.of(e));
        when(enrollmentRepository.countFailures(5L, 1L, PeriodStatus.CLOSED, 4.0)).thenReturn(1L);
        when(enrollmentRepository.findApprovedSubjectIds(5L, PeriodStatus.CLOSED, 4.0))
                .thenReturn(List.of());
        when(subjectRepository.findByStudyPlanId(10L)).thenReturn(List.of(subject1, subject2));

        ClosureResponse response = periodClosureService.close(1L);

        assertEquals(StudentStatus.REGULAR, student.getStatus());
        assertTrue(response.eliminatedRuns().isEmpty());
    }

    @Test
    void close_studentApprovedEverySubjectOfThePlan_isGraduated() {
        Enrollment e = enrollment(subject2, 6.0);
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period);
        when(enrollmentRepository.findByPeriodIdAndGradeIsNull(1L)).thenReturn(List.of());
        when(enrollmentRepository.findByPeriodId(1L)).thenReturn(List.of(e));
        when(enrollmentRepository.findApprovedSubjectIds(5L, PeriodStatus.CLOSED, 4.0))
                .thenReturn(List.of(1L, 2L));
        when(subjectRepository.findByStudyPlanId(10L)).thenReturn(List.of(subject1, subject2));

        ClosureResponse response = periodClosureService.close(1L);

        assertEquals(StudentStatus.GRADUATED, student.getStatus());
        assertEquals(List.of("12345678-K"), response.graduatedRuns());
        assertTrue(response.eliminatedRuns().isEmpty());
    }
}