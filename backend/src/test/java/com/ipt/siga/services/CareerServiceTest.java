package com.ipt.siga.services;

import com.ipt.siga.dtos.CareerRequest;
import com.ipt.siga.dtos.CareerResponse;
import com.ipt.siga.entities.Career;
import com.ipt.siga.enums.CareerStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.CareerRepository;
import com.ipt.siga.repositories.StudentRepository;
import com.ipt.siga.repositories.StudyPlanRepository;
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
class CareerServiceTest {

    @Mock
    private CareerRepository careerRepository;
    @Mock
    private StudyPlanRepository studyPlanRepository;
    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CareerService careerService;

    private Career career(Long id, CareerStatus status) {
        Career c = new Career();
        c.setId(id);
        c.setCode("10450");
        c.setName("Career " + id);
        c.setDescription("Description");
        c.setStatus(status);
        c.setDurationSemesters(4);
        return c;
    }

    // ---------- create ----------

    @Test
    void create_withoutStatus_startsActiveWithFourSemesters() {
        CareerRequest request = new CareerRequest("10450", "Programmer", "Description", null);
        when(careerRepository.existsByCode("10450")).thenReturn(false);
        when(careerRepository.save(any(Career.class))).thenAnswer(inv -> {
            Career c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CareerResponse response = careerService.create(request);

        assertEquals(1L, response.id());
        assertEquals(CareerStatus.ACTIVE, response.status());
        assertEquals(4, response.durationSemesters());
    }

    @Test
    void create_withExplicitStatus_keepsIt() {
        CareerRequest request = new CareerRequest("10450", "Programmer", "Description", CareerStatus.INACTIVE);
        when(careerRepository.existsByCode("10450")).thenReturn(false);
        when(careerRepository.save(any(Career.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(CareerStatus.INACTIVE, careerService.create(request).status());
    }

    @Test
    void create_duplicatedCode_throwsAndDoesNotSave() {
        CareerRequest request = new CareerRequest("10450", "Programmer", "Description", null);
        when(careerRepository.existsByCode("10450")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> careerService.create(request));

        verify(careerRepository, never()).save(any());
    }

    // ---------- find ----------

    @Test
    void findAll_returnsEveryCareer() {
        when(careerRepository.findAll()).thenReturn(List.of(
                career(1L, CareerStatus.ACTIVE), career(2L, CareerStatus.INACTIVE)));

        assertEquals(2, careerService.findAll().size());
    }

    @Test
    void findById_existing_returnsCareer() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, CareerStatus.ACTIVE)));

        assertEquals("Career 1", careerService.findById(1L).name());
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(careerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> careerService.findById(99L));
    }

    // ---------- update ----------

    @Test
    void update_withStatus_changesFieldsAndStatus() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, CareerStatus.ACTIVE)));
        when(careerRepository.existsByCodeAndIdNot("NEW", 1L)).thenReturn(false);
        when(careerRepository.save(any(Career.class))).thenAnswer(inv -> inv.getArgument(0));

        CareerResponse response = careerService.update(1L,
                new CareerRequest("NEW", "New name", "New description", CareerStatus.INACTIVE));

        assertEquals("NEW", response.code());
        assertEquals("New name", response.name());
        assertEquals(CareerStatus.INACTIVE, response.status());
    }

    @Test
    void update_withoutStatus_keepsCurrentStatus() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, CareerStatus.ACTIVE)));
        when(careerRepository.existsByCodeAndIdNot("NEW", 1L)).thenReturn(false);
        when(careerRepository.save(any(Career.class))).thenAnswer(inv -> inv.getArgument(0));

        CareerResponse response = careerService.update(1L,
                new CareerRequest("NEW", "New name", "New description", null));

        assertEquals(CareerStatus.ACTIVE, response.status());
    }

    @Test
    void update_duplicatedCode_throws() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, CareerStatus.ACTIVE)));
        when(careerRepository.existsByCodeAndIdNot("TAKEN", 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> careerService.update(1L,
                new CareerRequest("TAKEN", "Name", "Description", null)));

        verify(careerRepository, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void delete_withStudents_throws() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, CareerStatus.ACTIVE)));
        when(studentRepository.existsByCareerId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> careerService.delete(1L));

        verify(careerRepository, never()).delete(any());
    }

    @Test
    void delete_withStudyPlans_throws() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, CareerStatus.ACTIVE)));
        when(studentRepository.existsByCareerId(1L)).thenReturn(false);
        when(studyPlanRepository.existsByCareerId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> careerService.delete(1L));

        verify(careerRepository, never()).delete(any());
    }

    @Test
    void delete_withoutStudentsNorPlans_deletesCareer() {
        Career existing = career(1L, CareerStatus.ACTIVE);
        when(careerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByCareerId(1L)).thenReturn(false);
        when(studyPlanRepository.existsByCareerId(1L)).thenReturn(false);

        careerService.delete(1L);

        verify(careerRepository).delete(existing);
    }
}