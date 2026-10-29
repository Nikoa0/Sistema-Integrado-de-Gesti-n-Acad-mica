package com.ipt.siga.services;

import com.ipt.siga.dtos.MatriculationRequest;
import com.ipt.siga.dtos.MatriculationResponse;
import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Matriculation;
import com.ipt.siga.entities.Student;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.EnrollmentRepository;
import com.ipt.siga.repositories.MatriculationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatriculationServiceTest {

    @Mock
    private MatriculationRepository matriculationRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private AcademicPeriodService academicPeriodService;

    @InjectMocks
    private MatriculationService matriculationService;

    private Student student() {
        Student s = new Student();
        s.setId(5L);
        s.setFullName("Ana Perez");
        return s;
    }

    private AcademicPeriod period() {
        AcademicPeriod p = new AcademicPeriod();
        p.setId(1L);
        p.setAcademicYear(2028);
        p.setSemester(1);
        return p;
    }

    private Matriculation matriculation() {
        Matriculation m = new Matriculation();
        m.setId(10L);
        m.setStudent(student());
        m.setPeriod(period());
        return m;
    }

    // ---------- create ----------

    @Test
    void create_newMatriculation_isSaved() {
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period());
        when(studentService.getStudentOrThrow(5L)).thenReturn(student());
        when(matriculationRepository.existsByStudentIdAndPeriodId(5L, 1L)).thenReturn(false);
        when(matriculationRepository.save(any(Matriculation.class))).thenAnswer(inv -> {
            Matriculation m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        MatriculationResponse response = matriculationService.create(1L, new MatriculationRequest(5L));

        assertEquals(10L, response.id());
        assertEquals("Ana Perez", response.studentName());
        assertEquals("2028-1", response.periodName());
    }

    @Test
    void create_alreadyMatriculated_throwsAndDoesNotSave() {
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period());
        when(studentService.getStudentOrThrow(5L)).thenReturn(student());
        when(matriculationRepository.existsByStudentIdAndPeriodId(5L, 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> matriculationService.create(1L, new MatriculationRequest(5L)));

        verify(matriculationRepository, never()).save(any());
    }

    // ---------- find ----------

    @Test
    void findByPeriod_returnsMatriculationsOfThePeriod() {
        when(matriculationRepository.findByPeriodId(1L)).thenReturn(List.of(matriculation()));

        List<MatriculationResponse> result = matriculationService.findByPeriod(1L);

        assertEquals(1, result.size());
        verify(academicPeriodService).getPeriodOrThrow(1L);
    }

    // ---------- delete ----------

    @Test
    void delete_missing_throwsNotFound() {
        when(matriculationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> matriculationService.delete(99L));
    }

    @Test
    void delete_studentHasEnrollmentsInThePeriod_throws() {
        when(matriculationRepository.findById(10L)).thenReturn(Optional.of(matriculation()));
        when(enrollmentRepository.existsByStudentIdAndPeriodId(5L, 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> matriculationService.delete(10L));

        verify(matriculationRepository, never()).delete(any());
    }

    @Test
    void delete_withoutEnrollments_deletesMatriculation() {
        Matriculation existing = matriculation();
        when(matriculationRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.existsByStudentIdAndPeriodId(5L, 1L)).thenReturn(false);

        matriculationService.delete(10L);

        verify(matriculationRepository).delete(existing);
    }
}