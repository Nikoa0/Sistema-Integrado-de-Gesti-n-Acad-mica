package com.ipt.siga.services;

import com.ipt.siga.dtos.StudyPlanRequest;
import com.ipt.siga.dtos.StudyPlanResponse;
import com.ipt.siga.entities.Career;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.enums.StudyPlanStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.StudentRepository;
import com.ipt.siga.repositories.StudyPlanRepository;
import com.ipt.siga.repositories.SubjectRepository;
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
class StudyPlanServiceTest {

    @Mock
    private StudyPlanRepository studyPlanRepository;
    @Mock
    private CareerService careerService;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudyPlanService studyPlanService;

    private Career career(Long id) {
        Career c = new Career();
        c.setId(id);
        return c;
    }

    private StudyPlan plan(Long id, String identifier, StudyPlanStatus status) {
        StudyPlan p = new StudyPlan();
        p.setId(id);
        p.setCareer(career(1L));
        p.setIdentifier(identifier);
        p.setStatus(status);
        return p;
    }

    // ---------- create ----------

    @Test
    void create_withoutCurrentFlag_isNotCurrentAndDoesNotTouchOtherPlans() {
        when(careerService.getCareerOrThrow(1L)).thenReturn(career(1L));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2028.1")).thenReturn(false);
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        StudyPlanResponse response = studyPlanService.create(1L, new StudyPlanRequest("2028.1", null));

        assertEquals(StudyPlanStatus.NOT_CURRENT, response.status());
        verify(studyPlanRepository, never()).findByCareerIdAndStatus(any(), any());
    }

    @Test
    void create_asCurrent_demotesThePreviousCurrentPlan() {
        StudyPlan old = plan(5L, "2021.3", StudyPlanStatus.CURRENT);
        when(careerService.getCareerOrThrow(1L)).thenReturn(career(1L));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2028.1")).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT))
                .thenReturn(Optional.of(old));
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        StudyPlanResponse response = studyPlanService.create(1L, new StudyPlanRequest("2028.1", true));

        assertEquals(StudyPlanStatus.CURRENT, response.status());
        assertEquals(StudyPlanStatus.NOT_CURRENT, old.getStatus());
        verify(studyPlanRepository).saveAndFlush(old);
    }

    @Test
    void create_asCurrentWithoutPreviousCurrent_justSaves() {
        when(careerService.getCareerOrThrow(1L)).thenReturn(career(1L));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2028.1")).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT))
                .thenReturn(Optional.empty());
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        StudyPlanResponse response = studyPlanService.create(1L, new StudyPlanRequest("2028.1", true));

        assertEquals(StudyPlanStatus.CURRENT, response.status());
        verify(studyPlanRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_duplicatedIdentifier_throwsAndDoesNotSave() {
        when(careerService.getCareerOrThrow(1L)).thenReturn(career(1L));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2021.3")).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> studyPlanService.create(1L, new StudyPlanRequest("2021.3", true)));

        verify(studyPlanRepository, never()).save(any());
    }

    // ---------- find ----------

    @Test
    void findByCareer_returnsItsPlans() {
        when(studyPlanRepository.findByCareerId(1L)).thenReturn(List.of(
                plan(1L, "2021.3", StudyPlanStatus.CURRENT), plan(2L, "2019.1", StudyPlanStatus.NOT_CURRENT)));

        assertEquals(2, studyPlanService.findByCareer(1L).size());
        verify(careerService).getCareerOrThrow(1L);
    }

    @Test
    void findById_existing_returnsPlan() {
        when(studyPlanRepository.findById(1L))
                .thenReturn(Optional.of(plan(1L, "2021.3", StudyPlanStatus.CURRENT)));

        assertEquals("2021.3", studyPlanService.findById(1L).identifier());
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(studyPlanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studyPlanService.findById(99L));
    }

    // ---------- update ----------

    @Test
    void update_duplicatedIdentifier_throws() {
        when(studyPlanRepository.findById(1L))
                .thenReturn(Optional.of(plan(1L, "2021.3", StudyPlanStatus.CURRENT)));
        when(studyPlanRepository.existsByCareerIdAndIdentifierAndIdNot(1L, "2019.1", 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> studyPlanService.update(1L, new StudyPlanRequest("2019.1", null)));

        verify(studyPlanRepository, never()).save(any());
    }

    @Test
    void update_withoutCurrentFlag_onlyRenames() {
        StudyPlan existing = plan(1L, "2021.3", StudyPlanStatus.CURRENT);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studyPlanRepository.existsByCareerIdAndIdentifierAndIdNot(1L, "2021.4", 1L)).thenReturn(false);
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        StudyPlanResponse response = studyPlanService.update(1L, new StudyPlanRequest("2021.4", null));

        assertEquals("2021.4", response.identifier());
        assertEquals(StudyPlanStatus.CURRENT, response.status());
    }

    @Test
    void update_makeCurrent_demotesTheOtherCurrentPlan() {
        StudyPlan existing = plan(2L, "2028.1", StudyPlanStatus.NOT_CURRENT);
        StudyPlan other = plan(1L, "2021.3", StudyPlanStatus.CURRENT);
        when(studyPlanRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(studyPlanRepository.existsByCareerIdAndIdentifierAndIdNot(1L, "2028.1", 2L)).thenReturn(false);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT))
                .thenReturn(Optional.of(other));
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        StudyPlanResponse response = studyPlanService.update(2L, new StudyPlanRequest("2028.1", true));

        assertEquals(StudyPlanStatus.CURRENT, response.status());
        assertEquals(StudyPlanStatus.NOT_CURRENT, other.getStatus());
    }

    @Test
    void update_currentPlanMarkedCurrentAgain_changesNothing() {
        StudyPlan existing = plan(1L, "2021.3", StudyPlanStatus.CURRENT);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studyPlanRepository.existsByCareerIdAndIdentifierAndIdNot(1L, "2021.3", 1L)).thenReturn(false);
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        StudyPlanResponse response = studyPlanService.update(1L, new StudyPlanRequest("2021.3", true));

        assertEquals(StudyPlanStatus.CURRENT, response.status());
        verify(studyPlanRepository, never()).findByCareerIdAndStatus(any(), any());
    }

    @Test
    void update_currentFalse_setsNotCurrent() {
        StudyPlan existing = plan(1L, "2021.3", StudyPlanStatus.CURRENT);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studyPlanRepository.existsByCareerIdAndIdentifierAndIdNot(1L, "2021.3", 1L)).thenReturn(false);
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        StudyPlanResponse response = studyPlanService.update(1L, new StudyPlanRequest("2021.3", false));

        assertEquals(StudyPlanStatus.NOT_CURRENT, response.status());
    }

    // ---------- delete ----------

    @Test
    void delete_withStudents_throws() {
        when(studyPlanRepository.findById(1L))
                .thenReturn(Optional.of(plan(1L, "2021.3", StudyPlanStatus.CURRENT)));
        when(studentRepository.existsByStudyPlanId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> studyPlanService.delete(1L));

        verify(studyPlanRepository, never()).delete(any());
    }

    @Test
    void delete_withSubjects_throws() {
        when(studyPlanRepository.findById(1L))
                .thenReturn(Optional.of(plan(1L, "2021.3", StudyPlanStatus.CURRENT)));
        when(studentRepository.existsByStudyPlanId(1L)).thenReturn(false);
        when(subjectRepository.existsByStudyPlanId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> studyPlanService.delete(1L));

        verify(studyPlanRepository, never()).delete(any());
    }

    @Test
    void delete_withoutStudentsNorSubjects_deletesPlan() {
        StudyPlan existing = plan(1L, "2021.3", StudyPlanStatus.CURRENT);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByStudyPlanId(1L)).thenReturn(false);
        when(subjectRepository.existsByStudyPlanId(1L)).thenReturn(false);

        studyPlanService.delete(1L);

        verify(studyPlanRepository).delete(existing);
    }

    // ---------- getCurrentPlanOrThrow ----------

    @Test
    void getCurrentPlanOrThrow_existing_returnsPlan() {
        StudyPlan current = plan(1L, "2021.3", StudyPlanStatus.CURRENT);
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT))
                .thenReturn(Optional.of(current));

        assertSame(current, studyPlanService.getCurrentPlanOrThrow(1L));
    }

    @Test
    void getCurrentPlanOrThrow_missing_throwsBusinessRule() {
        when(studyPlanRepository.findByCareerIdAndStatus(1L, StudyPlanStatus.CURRENT))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> studyPlanService.getCurrentPlanOrThrow(1L));
    }
}