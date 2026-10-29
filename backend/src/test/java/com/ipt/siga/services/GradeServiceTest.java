package com.ipt.siga.services;

import com.ipt.siga.dtos.EnrollmentResponse;
import com.ipt.siga.dtos.GradeRequest;
import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Enrollment;
import com.ipt.siga.entities.Section;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.enums.EnrollmentResult;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ForbiddenException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private GradeService gradeService;

    private Teacher teacher(Long id) {
        Teacher t = new Teacher();
        t.setId(id);
        return t;
    }

    // Enrollment whose section is taught by sectionTeacherId, in a period with the given status
    private Enrollment enrollment(Long sectionTeacherId, PeriodStatus periodStatus) {
        Section section = new Section();
        section.setTeacher(teacher(sectionTeacherId));

        AcademicPeriod period = new AcademicPeriod();
        period.setStatus(periodStatus);

        Enrollment e = new Enrollment();
        e.setId(10L);
        e.setSection(section);
        e.setPeriod(period);
        return e;
    }

    @Test
    void register_responsibleTeacherInOpenPeriod_savesGrade() {
        Enrollment enrollment = enrollment(1L, PeriodStatus.OPEN);
        EnrollmentResponse expected = new EnrollmentResponse(10L, 1L, "Ana", 1L, "TAP101", "Prog",
                1L, 1L, "2028-1", LocalDate.now(), 5.5, EnrollmentResult.APPROVED);

        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrollment));
        when(accessControlService.currentTeacher()).thenReturn(teacher(1L));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);
        when(enrollmentService.toResponse(enrollment)).thenReturn(expected);

        EnrollmentResponse response = gradeService.register(10L, new GradeRequest(5.5));

        assertEquals(5.5, enrollment.getGrade());
        assertEquals(expected, response);
    }

    @Test
    void register_canModifyAnExistingGrade() {
        Enrollment enrollment = enrollment(1L, PeriodStatus.OPEN);
        enrollment.setGrade(3.0);

        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrollment));
        when(accessControlService.currentTeacher()).thenReturn(teacher(1L));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        gradeService.register(10L, new GradeRequest(6.0));

        assertEquals(6.0, enrollment.getGrade());
    }

    @Test
    void register_missingEnrollment_throwsNotFound() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gradeService.register(99L, new GradeRequest(5.0)));
    }

    @Test
    void register_teacherOfAnotherSection_throwsForbidden() {
        Enrollment enrollment = enrollment(2L, PeriodStatus.OPEN); // section belongs to teacher 2
        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrollment));
        when(accessControlService.currentTeacher()).thenReturn(teacher(1L)); // teacher 1 is logged in

        assertThrows(ForbiddenException.class,
                () -> gradeService.register(10L, new GradeRequest(5.0)));

        verify(enrollmentRepository, never()).save(any());
        assertNull(enrollment.getGrade());
    }

    @Test
    void register_closedPeriod_throwsBusinessRule() {
        Enrollment enrollment = enrollment(1L, PeriodStatus.CLOSED);
        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrollment));
        when(accessControlService.currentTeacher()).thenReturn(teacher(1L));

        assertThrows(BusinessRuleException.class,
                () -> gradeService.register(10L, new GradeRequest(5.0)));

        verify(enrollmentRepository, never()).save(any());
    }
}