package com.ipt.siga.services;

import com.ipt.siga.dtos.TeacherCreateRequest;
import com.ipt.siga.dtos.TeacherResponse;
import com.ipt.siga.dtos.TeacherUpdateRequest;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.enums.TeacherStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.SectionRepository;
import com.ipt.siga.repositories.TeacherRepository;
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
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private TeacherService teacherService;

    private Teacher teacher(Long id, TeacherStatus status) {
        Teacher t = new Teacher();
        t.setId(id);
        t.setRun("12345678-K");
        t.setFullName("Ana Perez");
        t.setEmail("ana@ipt.cl");
        t.setProfessionalTitle("Engineer");
        t.setAcademicDegree("Master");
        t.setStatus(status);
        return t;
    }

    private TeacherUpdateRequest updateRequest(String email, TeacherStatus status) {
        return new TeacherUpdateRequest("New Name", email, "New Title", "New Degree", status);
    }

    // ---------- create ----------

    @Test
    void create_normalizesDataAndStartsActive() {
        TeacherCreateRequest request = new TeacherCreateRequest(
                " 12345678-k ", " Ana Perez ", " ANA@IPT.CL ", " Engineer ", " Master ");
        when(teacherRepository.existsByRun("12345678-K")).thenReturn(false);
        when(teacherRepository.existsByEmail("ana@ipt.cl")).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> {
            Teacher t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TeacherResponse response = teacherService.create(request);

        assertEquals(1L, response.id());
        assertEquals("12345678-K", response.run());
        assertEquals("ana@ipt.cl", response.email());
        assertEquals("Ana Perez", response.fullName());
        assertEquals(TeacherStatus.ACTIVE, response.status());
    }

    @Test
    void create_duplicatedRun_throwsAndDoesNotSave() {
        TeacherCreateRequest request = new TeacherCreateRequest(
                "12345678-K", "Ana", "ana@ipt.cl", "Engineer", "Master");
        when(teacherRepository.existsByRun("12345678-K")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> teacherService.create(request));

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void create_duplicatedEmail_throwsAndDoesNotSave() {
        TeacherCreateRequest request = new TeacherCreateRequest(
                "12345678-K", "Ana", "ana@ipt.cl", "Engineer", "Master");
        when(teacherRepository.existsByRun("12345678-K")).thenReturn(false);
        when(teacherRepository.existsByEmail("ana@ipt.cl")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> teacherService.create(request));

        verify(teacherRepository, never()).save(any());
    }

    // ---------- find ----------

    @Test
    void findAll_withoutStatus_returnsEveryTeacher() {
        when(teacherRepository.findAll()).thenReturn(List.of(
                teacher(1L, TeacherStatus.ACTIVE), teacher(2L, TeacherStatus.INACTIVE)));

        assertEquals(2, teacherService.findAll(null).size());
    }

    @Test
    void findAll_withStatus_filtersByStatus() {
        when(teacherRepository.findByStatus(TeacherStatus.ACTIVE))
                .thenReturn(List.of(teacher(1L, TeacherStatus.ACTIVE)));

        List<TeacherResponse> result = teacherService.findAll(TeacherStatus.ACTIVE);

        assertEquals(1, result.size());
        verify(teacherRepository, never()).findAll();
    }

    @Test
    void findById_existing_returnsTeacher() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, TeacherStatus.ACTIVE)));

        assertEquals("Ana Perez", teacherService.findById(1L).fullName());
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(teacherRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teacherService.findById(99L));
    }

    // ---------- update ----------

    @Test
    void update_withoutStatus_keepsCurrentStatus() {
        Teacher existing = teacher(1L, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.existsByEmailAndIdNot("new@ipt.cl", 1L)).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherResponse response = teacherService.update(1L, updateRequest(" NEW@ipt.cl ", null));

        assertEquals("new@ipt.cl", response.email());
        assertEquals("New Name", response.fullName());
        assertEquals(TeacherStatus.ACTIVE, response.status());
    }

    @Test
    void update_toInactiveWithoutOpenSections_changesStatus() {
        Teacher existing = teacher(1L, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(sectionRepository.existsByTeacherIdAndPeriodStatus(1L, PeriodStatus.OPEN)).thenReturn(false);
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherResponse response = teacherService.update(1L, updateRequest("ana@ipt.cl", TeacherStatus.INACTIVE));

        assertEquals(TeacherStatus.INACTIVE, response.status());
    }

    @Test
    void update_toInactiveWithOpenSections_throws() {
        Teacher existing = teacher(1L, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(sectionRepository.existsByTeacherIdAndPeriodStatus(1L, PeriodStatus.OPEN)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> teacherService.update(1L, updateRequest("ana@ipt.cl", TeacherStatus.INACTIVE)));

        verify(teacherRepository, never()).save(any());
    }

    @Test
    void update_duplicatedEmail_throws() {
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher(1L, TeacherStatus.ACTIVE)));
        when(teacherRepository.existsByEmailAndIdNot("taken@ipt.cl", 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> teacherService.update(1L, updateRequest("taken@ipt.cl", null)));
    }

    // ---------- delete ----------

    @Test
    void delete_withSections_throws() {
        Teacher existing = teacher(1L, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sectionRepository.existsByTeacherId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> teacherService.delete(1L));

        verify(teacherRepository, never()).delete(any());
    }

    @Test
    void delete_withoutSections_deletesTeacher() {
        Teacher existing = teacher(1L, TeacherStatus.ACTIVE);
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sectionRepository.existsByTeacherId(1L)).thenReturn(false);

        teacherService.delete(1L);

        verify(teacherRepository).delete(existing);
    }
}