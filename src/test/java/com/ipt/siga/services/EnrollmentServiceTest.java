package com.ipt.siga.services;

import com.ipt.siga.dtos.EnrollmentRequest;
import com.ipt.siga.dtos.EnrollmentResponse;
import com.ipt.siga.dtos.SectionResponse;
import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Enrollment;
import com.ipt.siga.entities.ScheduleBlock;
import com.ipt.siga.entities.Section;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.enums.EnrollmentResult;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.enums.StudentStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.EnrollmentRepository;
import com.ipt.siga.repositories.MatriculationRepository;
import com.ipt.siga.repositories.SectionRepository;
import com.ipt.siga.repositories.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private MatriculationRepository matriculationRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private AcademicPeriodService academicPeriodService;
    @Mock
    private SectionService sectionService;
    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private StudyPlan plan;
    private Student student;
    private AcademicPeriod period;
    private Subject s1, s2, s3, s4;          // s1-s3 semester 1, s4 semester 2
    private Section sec1, sec2, sec3, sec4;

    @BeforeEach
    void setUp() {
        plan = new StudyPlan();
        plan.setId(1L);

        student = new Student();
        student.setId(5L);
        student.setFullName("Ana Perez");
        student.setStatus(StudentStatus.REGULAR);
        student.setStudyPlan(plan);

        period = new AcademicPeriod();
        period.setId(1L);
        period.setAcademicYear(2028);
        period.setSemester(1);
        period.setStatus(PeriodStatus.OPEN);

        s1 = subject(1L, "TAP101", 1);
        s2 = subject(2L, "TAP102", 1);
        s3 = subject(3L, "TAP103", 1);
        s4 = subject(4L, "TAP201", 2);

        sec1 = section(11L, s1, 30, "L1", "L2");
        sec2 = section(12L, s2, 30, "M1", "M2");
        sec3 = section(13L, s3, 30, "W1", "W2");
        sec4 = section(14L, s4, 30, "J1", "J2");
    }

    // ---------- helpers ----------

    private Subject subject(Long id, String code, int semester) {
        Subject s = new Subject();
        s.setId(id);
        s.setCode(code);
        s.setName("Name " + code);
        s.setSemester(semester);
        s.setStudyPlan(plan);
        return s;
    }

    private Section section(Long id, Subject subject, int capacity, String... codes) {
        Section s = new Section();
        s.setId(id);
        s.setSubject(subject);
        s.setPeriod(period);
        s.setCapacity(capacity);
        Set<ScheduleBlock> blocks = new HashSet<>();
        for (String code : codes) {
            blocks.add(ScheduleBlock.fromCode(code));
        }
        s.setBlocks(blocks);
        return s;
    }

    private Enrollment enrollment(Long id, Subject subject, Double grade) {
        Enrollment e = new Enrollment();
        e.setId(id);
        e.setStudent(student);
        e.setSubject(subject);
        e.setSection(section(100L + id, subject, 30, "V1", "V2"));
        e.setPeriod(period);
        e.setEnrolledAt(LocalDate.now());
        e.setGrade(grade);
        return e;
    }

    private EnrollmentRequest request(Long... subjectIds) {
        return new EnrollmentRequest(1L, Arrays.asList(subjectIds));
    }

    // Base scenario: valid student, open period, offer of s1..s4, nothing approved or enrolled yet
    private void stubScenario(Long... requestedIds) {
        List<Long> ids = Arrays.asList(requestedIds);
        List<Subject> allSubjects = List.of(s1, s2, s3, s4);
        List<Section> allSections = List.of(sec1, sec2, sec3, sec4);

        when(studentService.getStudentOrThrow(5L)).thenReturn(student);
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period);
        when(matriculationRepository.existsByStudentIdAndPeriodId(5L, 1L)).thenReturn(true);
        when(subjectRepository.findAllById(any())).thenReturn(
                allSubjects.stream().filter(s -> ids.contains(s.getId())).toList());
        when(sectionRepository.findByPeriodIdAndSubjectIdInOrderById(eq(1L), any())).thenReturn(
                allSections.stream().filter(s -> ids.contains(s.getSubject().getId())).toList());
        when(sectionRepository.findByPeriodId(1L)).thenReturn(allSections);
        when(enrollmentRepository.findByStudentIdAndPeriodId(5L, 1L)).thenReturn(new ArrayList<>());
        when(enrollmentRepository.findApprovedSubjectIds(5L, PeriodStatus.CLOSED, 4.0)).thenReturn(List.of());
        when(subjectRepository.findByStudyPlanId(1L)).thenReturn(allSubjects);
        when(enrollmentRepository.countBySectionId(anyLong())).thenReturn(0L);
        when(enrollmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- enroll: successful ----------

    @Test
    void enroll_validRequest_createsOneEnrollmentPerSubject() {
        stubScenario(1L, 2L, 3L);

        List<EnrollmentResponse> result = enrollmentService.enroll(5L, request(1L, 2L, 3L));

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(r -> r.studentId().equals(5L) && r.grade() == null));
        assertEquals("2028-1", result.get(0).periodName());
        verify(accessControlService).requireStudentAccess(5L);
        verify(enrollmentRepository).saveAll(any());
    }

    @Test
    void enroll_fewerThanThreeSubjectsLeftInThePlan_allowsLessThanThree() {
        stubScenario(1L, 2L);
        // The plan only has 2 subjects, so the minimum becomes the number of available ones
        when(subjectRepository.findByStudyPlanId(1L)).thenReturn(List.of(s1, s2));

        assertEquals(2, enrollmentService.enroll(5L, request(1L, 2L)).size());
    }

    @Test
    void enroll_approvedPrerequisite_isAccepted() {
        stubScenario(1L, 2L, 4L);
        s4.getPrerequisites().add(s1);
        when(enrollmentRepository.findApprovedSubjectIds(5L, PeriodStatus.CLOSED, 4.0))
                .thenReturn(List.of(1L));
        // s1 is already approved, so the pending subjects are s2, s3, s4 and s3 is still available...
        // ...but s3 is semester 1 and the highest requested is semester 2, so request s3 as well.
        stubScenario(2L, 3L, 4L);
        when(enrollmentRepository.findApprovedSubjectIds(5L, PeriodStatus.CLOSED, 4.0))
                .thenReturn(List.of(1L));

        assertEquals(3, enrollmentService.enroll(5L, request(2L, 3L, 4L)).size());
    }

    // ---------- enroll: rejections ----------

    @Test
    void enroll_closedPeriod_throws() {
        stubScenario(1L, 2L, 3L);
        period.setStatus(PeriodStatus.CLOSED);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 2L, 3L)));

        verify(enrollmentRepository, never()).saveAll(any());
    }

    @Test
    void enroll_studentNotRegular_throws() {
        stubScenario(1L, 2L, 3L);
        student.setStatus(StudentStatus.POSTPONED);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 2L, 3L)));
    }

    @Test
    void enroll_notMatriculatedInThePeriod_throws() {
        stubScenario(1L, 2L, 3L);
        when(matriculationRepository.existsByStudentIdAndPeriodId(5L, 1L)).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 2L, 3L)));
    }

    @Test
    void enroll_repeatedSubjectsInTheRequest_throws() {
        stubScenario(1L, 2L);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 1L, 2L)));
    }

    @Test
    void enroll_unknownSubject_throwsNotFound() {
        stubScenario(1L, 2L); // the repository only finds 2 of the 3 requested ids

        assertThrows(ResourceNotFoundException.class, () -> enrollmentService.enroll(5L, request(1L, 2L, 99L)));
    }

    @Test
    void enroll_subjectOfAnotherStudyPlan_throws() {
        stubScenario(1L, 2L, 3L);
        StudyPlan otherPlan = new StudyPlan();
        otherPlan.setId(2L);
        s3.setStudyPlan(otherPlan);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 2L, 3L)));
    }

    @Test
    void enroll_subjectNotOfferedInThePeriod_throws() {
        stubScenario(1L, 2L, 3L);
        when(sectionRepository.findByPeriodIdAndSubjectIdInOrderById(eq(1L), any()))
                .thenReturn(List.of(sec1, sec2)); // no section for s3

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 2L, 3L)));
    }

    @Test
    void enroll_subjectAlreadyEnrolledInThePeriod_throws() {
        stubScenario(1L, 2L, 3L);
        when(enrollmentRepository.findByStudentIdAndPeriodId(5L, 1L))
                .thenReturn(new ArrayList<>(List.of(enrollment(1L, s1, null))));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 2L, 3L)));
    }

    @Test
    void enroll_subjectAlreadyApproved_throws() {
        stubScenario(1L, 2L, 3L);
        when(enrollmentRepository.findApprovedSubjectIds(5L, PeriodStatus.CLOSED, 4.0))
                .thenReturn(List.of(1L));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 2L, 3L)));
    }

    @Test
    void enroll_missingPrerequisite_throws() {
        stubScenario(2L, 3L, 4L);
        s4.getPrerequisites().add(s1); // s1 is not approved

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> enrollmentService.enroll(5L, request(2L, 3L, 4L)));

        assertTrue(ex.getMessage().contains("TAP101"));
    }

    @Test
    void enroll_moreThanSixSubjects_throws() {
        stubScenario(1L, 2L, 3L);
        when(enrollmentRepository.findByStudentIdAndPeriodId(5L, 1L)).thenReturn(new ArrayList<>(List.of(
                enrollment(1L, subject(10L, "X1", 1), null), enrollment(2L, subject(11L, "X2", 1), null),
                enrollment(3L, subject(12L, "X3", 1), null), enrollment(4L, subject(13L, "X4", 1), null))));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 2L, 3L)));
    }

    @Test
    void enroll_lessThanThreeSubjectsWhenMoreArePending_throws() {
        stubScenario(1L, 2L);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 2L)));
    }

    @Test
    void enroll_higherSemesterWhileLowerOneIsPendingAndAvailable_throws() {
        stubScenario(2L, 3L, 4L); // s1 (semester 1) is pending and offered, but s4 is semester 2

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> enrollmentService.enroll(5L, request(2L, 3L, 4L)));

        assertTrue(ex.getMessage().contains("TAP101"));
    }

    @Test
    void enroll_sectionWithoutSeats_throws() {
        stubScenario(1L, 2L, 3L);
        when(enrollmentRepository.countBySectionId(11L)).thenReturn(30L);

        assertThrows(BusinessRuleException.class, () -> enrollmentService.enroll(5L, request(1L, 2L, 3L)));
    }

    @Test
    void enroll_scheduleConflictBetweenSections_throws() {
        stubScenario(1L, 2L, 3L);
        sec2.getBlocks().add(ScheduleBlock.fromCode("L1")); // same block as sec1

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> enrollmentService.enroll(5L, request(1L, 2L, 3L)));

        assertTrue(ex.getMessage().contains("L1"));
        verify(enrollmentRepository, never()).saveAll(any());
    }

    // ---------- cancel ----------

    @Test
    void cancel_missingEnrollment_throwsNotFound() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> enrollmentService.cancel(99L));
    }

    @Test
    void cancel_closedPeriod_throws() {
        Enrollment e = enrollment(1L, s1, null);
        period.setStatus(PeriodStatus.CLOSED);
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));

        assertThrows(BusinessRuleException.class, () -> enrollmentService.cancel(1L));

        verify(enrollmentRepository, never()).delete(any());
    }

    @Test
    void cancel_openPeriod_deletesEnrollment() {
        Enrollment e = enrollment(1L, s1, null);
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));

        enrollmentService.cancel(1L);

        verify(accessControlService).requireStudentAccess(5L);
        verify(enrollmentRepository).delete(e);
    }

    // ---------- queries ----------

    @Test
    void findOffer_usesTheStudyPlanOfTheStudent() {
        when(studentService.getStudentOrThrow(5L)).thenReturn(student);
        List<SectionResponse> offer = List.of(mock(SectionResponse.class));
        when(sectionService.findOffer(1L, 1L)).thenReturn(offer);

        assertSame(offer, enrollmentService.findOffer(5L, 1L));

        verify(accessControlService).requireStudentAccess(5L);
        verify(academicPeriodService).getPeriodOrThrow(1L);
    }

    @Test
    void findByStudent_withoutPeriod_returnsAllHisEnrollments() {
        when(enrollmentRepository.findByStudentId(5L)).thenReturn(List.of(enrollment(1L, s1, 5.0)));

        assertEquals(1, enrollmentService.findByStudent(5L, null).size());

        verify(enrollmentRepository, never()).findByStudentIdAndPeriodId(any(), any());
    }

    @Test
    void findByStudent_withPeriod_filtersByPeriod() {
        when(enrollmentRepository.findByStudentIdAndPeriodId(5L, 1L)).thenReturn(List.of(enrollment(1L, s1, 5.0)));

        assertEquals(1, enrollmentService.findByStudent(5L, 1L).size());

        verify(enrollmentRepository, never()).findByStudentId(any());
    }

    @Test
    void findByPeriod_returnsTheEnrollmentsOfThePeriod() {
        when(enrollmentRepository.findByPeriodId(1L)).thenReturn(List.of(enrollment(1L, s1, null)));

        assertEquals(1, enrollmentService.findByPeriod(1L).size());

        verify(academicPeriodService).getPeriodOrThrow(1L);
    }

    @Test
    void findBySection_checksAccessToTheSection() {
        when(sectionService.getSectionOrThrow(11L)).thenReturn(sec1);
        when(enrollmentRepository.findBySectionId(11L)).thenReturn(List.of(enrollment(1L, s1, null)));

        assertEquals(1, enrollmentService.findBySection(11L).size());

        verify(accessControlService).requireSectionAccess(sec1);
    }

    // ---------- toResponse ----------

    @Test
    void toResponse_gradeAtLeastFour_isApproved() {
        assertEquals(EnrollmentResult.APPROVED, enrollmentService.toResponse(enrollment(1L, s1, 4.0)).result());
    }

    @Test
    void toResponse_gradeBelowFour_isFailed() {
        assertEquals(EnrollmentResult.FAILED, enrollmentService.toResponse(enrollment(1L, s1, 3.9)).result());
    }

    @Test
    void toResponse_withoutGrade_hasNoResult() {
        assertNull(enrollmentService.toResponse(enrollment(1L, s1, null)).result());
    }
}