package com.ipt.siga.services;

import com.ipt.siga.dtos.SubjectRequest;
import com.ipt.siga.dtos.SubjectResponse;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.SectionRepository;
import com.ipt.siga.repositories.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private StudyPlanService studyPlanService;
    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private SubjectService subjectService;

    private final StudyPlan plan = buildPlan();

    private static StudyPlan buildPlan() {
        StudyPlan p = new StudyPlan();
        p.setId(1L);
        return p;
    }

    private Subject subject(Long id, String code, int semester) {
        Subject s = new Subject();
        s.setId(id);
        s.setStudyPlan(plan);
        s.setCode(code);
        s.setName("Name " + code);
        s.setSemester(semester);
        s.setTheoryHours(2);
        s.setExerciseHours(0);
        s.setLabHours(2);
        s.setSct(5);
        s.setPrerequisites(new HashSet<>());
        return s;
    }

    private SubjectRequest request(String code, int semester, int theory, int exercise, int lab,
                                   Set<String> prerequisites) {
        return new SubjectRequest(code, "Name", semester, theory, exercise, lab, 5, prerequisites);
    }

    private void stubSave() {
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> {
            Subject s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });
    }

    // ---------- create ----------

    @Test
    void create_withoutPrerequisites_savesSubject() {
        when(studyPlanService.getPlanOrThrow(1L)).thenReturn(plan);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(false);
        stubSave();

        SubjectResponse response = subjectService.create(1L, request("TAP101", 1, 2, 2, 2, null));

        assertEquals(100L, response.id());
        assertEquals("TAP101", response.code());
        assertTrue(response.prerequisiteCodes().isEmpty());
    }

    @Test
    void create_withEmptyPrerequisiteSet_savesSubject() {
        when(studyPlanService.getPlanOrThrow(1L)).thenReturn(plan);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(false);
        stubSave();

        SubjectResponse response = subjectService.create(1L, request("TAP101", 1, 2, 2, 2, Set.of()));

        assertTrue(response.prerequisiteCodes().isEmpty());
    }

    @Test
    void create_withValidPrerequisites_linksThemSorted() {
        Set<String> codes = Set.of("TAP102", "TAP101");
        when(studyPlanService.getPlanOrThrow(1L)).thenReturn(plan);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(false);
        when(subjectRepository.findByStudyPlanIdAndCodeIn(1L, codes))
                .thenReturn(List.of(subject(1L, "TAP101", 1), subject(2L, "TAP102", 1)));
        stubSave();

        SubjectResponse response = subjectService.create(1L, request("TAP201", 2, 2, 2, 2, codes));

        assertEquals(List.of("TAP101", "TAP102"), response.prerequisiteCodes());
    }

    @Test
    void create_duplicatedCode_throwsAndDoesNotSave() {
        when(studyPlanService.getPlanOrThrow(1L)).thenReturn(plan);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> subjectService.create(1L, request("TAP101", 1, 2, 2, 2, null)));

        verify(subjectRepository, never()).save(any());
    }

    @Test
    void create_weeklyHoursEqualToEight_throws() {
        when(studyPlanService.getPlanOrThrow(1L)).thenReturn(plan);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP101")).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> subjectService.create(1L, request("TAP101", 1, 4, 2, 2, null)));

        verify(subjectRepository, never()).save(any());
    }

    @Test
    void create_moreThanThreePrerequisites_throws() {
        when(studyPlanService.getPlanOrThrow(1L)).thenReturn(plan);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP401")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> subjectService.create(1L,
                request("TAP401", 4, 2, 2, 2, Set.of("A", "B", "C", "D"))));

        verify(subjectRepository, never()).save(any());
    }

    @Test
    void create_prerequisiteNotFound_throwsAndNamesTheMissingCode() {
        Set<String> codes = Set.of("TAP101", "TAP999");
        when(studyPlanService.getPlanOrThrow(1L)).thenReturn(plan);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(false);
        when(subjectRepository.findByStudyPlanIdAndCodeIn(1L, codes))
                .thenReturn(List.of(subject(1L, "TAP101", 1)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> subjectService.create(1L, request("TAP201", 2, 2, 2, 2, codes)));

        assertTrue(ex.getMessage().contains("TAP999"));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void create_prerequisiteInSameSemester_throws() {
        Set<String> codes = Set.of("TAP202");
        when(studyPlanService.getPlanOrThrow(1L)).thenReturn(plan);
        when(subjectRepository.existsByStudyPlanIdAndCode(1L, "TAP201")).thenReturn(false);
        when(subjectRepository.findByStudyPlanIdAndCodeIn(1L, codes))
                .thenReturn(List.of(subject(2L, "TAP202", 2)));

        assertThrows(BusinessRuleException.class,
                () -> subjectService.create(1L, request("TAP201", 2, 2, 2, 2, codes)));

        verify(subjectRepository, never()).save(any());
    }

    // ---------- find ----------

    @Test
    void findByPlan_withoutSemester_returnsAllOfThePlan() {
        when(subjectRepository.findByStudyPlanId(1L))
                .thenReturn(List.of(subject(1L, "TAP101", 1), subject(2L, "TAP201", 2)));

        assertEquals(2, subjectService.findByPlan(1L, null).size());
        verify(subjectRepository, never()).findByStudyPlanIdAndSemester(any(), any());
    }

    @Test
    void findByPlan_withSemester_filtersBySemester() {
        when(subjectRepository.findByStudyPlanIdAndSemester(1L, 2))
                .thenReturn(List.of(subject(2L, "TAP201", 2)));

        List<SubjectResponse> result = subjectService.findByPlan(1L, 2);

        assertEquals(1, result.size());
        verify(subjectRepository, never()).findByStudyPlanId(any());
    }

    @Test
    void findById_existing_returnsSubject() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "TAP101", 1)));

        assertEquals("TAP101", subjectService.findById(1L).code());
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> subjectService.findById(99L));
    }

    // ---------- update ----------

    @Test
    void update_validData_updatesSubject() {
        Subject existing = subject(1L, "TAP201", 2);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(subjectRepository.existsByStudyPlanIdAndCodeAndIdNot(1L, "TAP201", 1L)).thenReturn(false);
        when(subjectRepository.findDependents(1L)).thenReturn(List.of());
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = subjectService.update(1L, request("TAP201", 2, 2, 0, 2, null));

        assertEquals("Name", response.name());
        assertEquals(2, response.semester());
    }

    @Test
    void update_duplicatedCode_throws() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "TAP201", 2)));
        when(subjectRepository.existsByStudyPlanIdAndCodeAndIdNot(1L, "TAP101", 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> subjectService.update(1L, request("TAP101", 2, 2, 0, 2, null)));

        verify(subjectRepository, never()).save(any());
    }

    @Test
    void update_weeklyHoursTooHigh_throws() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "TAP201", 2)));
        when(subjectRepository.existsByStudyPlanIdAndCodeAndIdNot(1L, "TAP201", 1L)).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> subjectService.update(1L, request("TAP201", 2, 4, 2, 2, null)));

        verify(subjectRepository, never()).save(any());
    }

    @Test
    void update_dependentWouldNotBeInALaterSemester_throws() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "TAP101", 1)));
        when(subjectRepository.existsByStudyPlanIdAndCodeAndIdNot(1L, "TAP101", 1L)).thenReturn(false);
        when(subjectRepository.findDependents(1L)).thenReturn(List.of(subject(2L, "TAP201", 2)));

        // Moving TAP101 to semester 2 leaves its dependent TAP201 (semester 2) in the same semester
        assertThrows(BusinessRuleException.class,
                () -> subjectService.update(1L, request("TAP101", 2, 2, 0, 2, null)));

        verify(subjectRepository, never()).save(any());
    }

    @Test
    void update_dependentStaysInALaterSemester_isAllowed() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "TAP101", 1)));
        when(subjectRepository.existsByStudyPlanIdAndCodeAndIdNot(1L, "TAP101", 1L)).thenReturn(false);
        when(subjectRepository.findDependents(1L)).thenReturn(List.of(subject(2L, "TAP301", 3)));
        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = subjectService.update(1L, request("TAP101", 2, 2, 0, 2, null));

        assertEquals(2, response.semester());
    }

    // ---------- delete ----------

    @Test
    void delete_withSections_throws() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "TAP101", 1)));
        when(sectionRepository.existsBySubjectId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> subjectService.delete(1L));

        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void delete_prerequisiteOfOtherSubjects_throws() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject(1L, "TAP101", 1)));
        when(sectionRepository.existsBySubjectId(1L)).thenReturn(false);
        when(subjectRepository.findDependents(1L)).thenReturn(List.of(subject(2L, "TAP201", 2)));

        assertThrows(BusinessRuleException.class, () -> subjectService.delete(1L));

        verify(subjectRepository, never()).delete(any());
    }

    @Test
    void delete_free_deletesSubject() {
        Subject existing = subject(1L, "TAP101", 1);
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(sectionRepository.existsBySubjectId(1L)).thenReturn(false);
        when(subjectRepository.findDependents(1L)).thenReturn(List.of());

        subjectService.delete(1L);

        verify(subjectRepository).delete(existing);
    }
}