package com.ipt.siga.services;

import com.ipt.siga.dtos.AcademicPeriodRequest;
import com.ipt.siga.dtos.AcademicPeriodResponse;
import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.AcademicPeriodRepository;
import com.ipt.siga.repositories.MatriculationRepository;
import com.ipt.siga.repositories.SectionRepository;
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
class AcademicPeriodServiceTest {

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private MatriculationRepository matriculationRepository;

    @InjectMocks
    private AcademicPeriodService academicPeriodService;

    private AcademicPeriod period(Long id, int year, int semester, PeriodStatus status) {
        AcademicPeriod p = new AcademicPeriod();
        p.setId(id);
        p.setAcademicYear(year);
        p.setSemester(semester);
        p.setStatus(status);
        return p;
    }

    // ---------- create ----------

    @Test
    void create_newPeriod_startsOpen() {
        when(academicPeriodRepository.existsByAcademicYearAndSemester(2028, 1)).thenReturn(false);
        when(academicPeriodRepository.save(any(AcademicPeriod.class))).thenAnswer(inv -> {
            AcademicPeriod p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        AcademicPeriodResponse response = academicPeriodService.create(new AcademicPeriodRequest(2028, 1));

        assertEquals(PeriodStatus.OPEN, response.status());
        assertEquals("2028-1", response.name());
        assertEquals(1L, response.id());
    }

    @Test
    void create_duplicatedPeriod_throwsAndDoesNotSave() {
        when(academicPeriodRepository.existsByAcademicYearAndSemester(2028, 1)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> academicPeriodService.create(new AcademicPeriodRequest(2028, 1)));

        verify(academicPeriodRepository, never()).save(any());
    }

    // ---------- find ----------

    @Test
    void findAll_returnsPeriods() {
        when(academicPeriodRepository.findAllByOrderByAcademicYearDescSemesterDesc()).thenReturn(List.of(
                period(2L, 2028, 2, PeriodStatus.OPEN), period(1L, 2028, 1, PeriodStatus.CLOSED)));

        List<AcademicPeriodResponse> result = academicPeriodService.findAll();

        assertEquals(2, result.size());
        assertEquals("2028-2", result.get(0).name());
    }

    @Test
    void findById_existing_returnsPeriod() {
        when(academicPeriodRepository.findById(1L))
                .thenReturn(Optional.of(period(1L, 2028, 1, PeriodStatus.OPEN)));

        assertEquals("2028-1", academicPeriodService.findById(1L).name());
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(academicPeriodRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> academicPeriodService.findById(99L));
    }

    // ---------- delete ----------

    @Test
    void delete_withMatriculations_throws() {
        when(academicPeriodRepository.findById(1L))
                .thenReturn(Optional.of(period(1L, 2028, 1, PeriodStatus.OPEN)));
        when(matriculationRepository.existsByPeriodId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> academicPeriodService.delete(1L));

        verify(academicPeriodRepository, never()).delete(any());
    }

    @Test
    void delete_withSections_throws() {
        when(academicPeriodRepository.findById(1L))
                .thenReturn(Optional.of(period(1L, 2028, 1, PeriodStatus.OPEN)));
        when(matriculationRepository.existsByPeriodId(1L)).thenReturn(false);
        when(sectionRepository.existsByPeriodId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> academicPeriodService.delete(1L));

        verify(academicPeriodRepository, never()).delete(any());
    }

    @Test
    void delete_withoutMatriculationsNorSections_deletesPeriod() {
        AcademicPeriod existing = period(1L, 2028, 1, PeriodStatus.OPEN);
        when(academicPeriodRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(matriculationRepository.existsByPeriodId(1L)).thenReturn(false);
        when(sectionRepository.existsByPeriodId(1L)).thenReturn(false);

        academicPeriodService.delete(1L);

        verify(academicPeriodRepository).delete(existing);
    }
}