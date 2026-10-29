package com.ipt.siga.services;

import com.ipt.siga.dtos.StudentCreateRequest;
import com.ipt.siga.dtos.StudentResponse;
import com.ipt.siga.dtos.StudentUpdateRequest;
import com.ipt.siga.entities.Career;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.enums.CareerStatus;
import com.ipt.siga.enums.StudentStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.EnrollmentRepository;
import com.ipt.siga.repositories.MatriculationRepository;
import com.ipt.siga.repositories.StudentRepository;
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
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CareerService careerService;
    @Mock
    private StudyPlanService studyPlanService;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private MatriculationRepository matriculationRepository;
    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private StudentService studentService;

    // ---------- helpers ----------

    private Career career(Long id, CareerStatus status) {
        Career c = new Career();
        c.setId(id);
        c.setName("Career " + id);
        c.setStatus(status);
        return c;
    }

    private StudyPlan plan(Long id, Career career) {
        StudyPlan p = new StudyPlan();
        p.setId(id);
        p.setCareer(career);
        p.setIdentifier("2021." + id);
        return p;
    }

    private Student student(Long id, Career career, StudyPlan plan, StudentStatus status) {
        Student s = new Student();
        s.setId(id);
        s.setRun("12345678-K");
        s.setFullName("Ana Perez");
        s.setEmail("ana@ipt.cl");
        s.setCareer(career);
        s.setStudyPlan(plan);
        s.setStatus(status);
        return s;
    }

    // A REGULAR student of career 1 / plan 1
    private Student regularStudent() {
        Career c1 = career(1L, CareerStatus.ACTIVE);
        return student(1L, c1, plan(1L, c1), StudentStatus.REGULAR);
    }

    private StudentUpdateRequest updateRequest(String email, Long careerId, Long planId, StudentStatus status) {
        return new StudentUpdateRequest(" New Name ", email, careerId, planId, status);
    }

    private void stubSave() {
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- create ----------

    @Test
    void create_validData_normalizesAndStartsRegularInCurrentPlan() {
        Career c1 = career(1L, CareerStatus.ACTIVE);
        StudyPlan current = plan(7L, c1);
        when(careerService.getCareerOrThrow(1L)).thenReturn(c1);
        when(studentRepository.existsByRun("12345678-K")).thenReturn(false);
        when(studentRepository.existsByEmail("ana@ipt.cl")).thenReturn(false);
        when(studyPlanService.getCurrentPlanOrThrow(1L)).thenReturn(current);
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        StudentResponse response = studentService.create(
                new StudentCreateRequest(" 12345678-k ", " Ana Perez ", " ANA@IPT.CL ", 1L));

        assertEquals("12345678-K", response.run());
        assertEquals("ana@ipt.cl", response.email());
        assertEquals("Ana Perez", response.fullName());
        assertEquals(StudentStatus.REGULAR, response.status());
        assertEquals(7L, response.studyPlanId());
    }

    @Test
    void create_duplicatedRun_throwsAndDoesNotSave() {
        when(careerService.getCareerOrThrow(1L)).thenReturn(career(1L, CareerStatus.ACTIVE));
        when(studentRepository.existsByRun("12345678-K")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> studentService.create(
                new StudentCreateRequest("12345678-K", "Ana", "ana@ipt.cl", 1L)));

        verify(studentRepository, never()).save(any());
    }

    @Test
    void create_duplicatedEmail_throwsAndDoesNotSave() {
        when(careerService.getCareerOrThrow(1L)).thenReturn(career(1L, CareerStatus.ACTIVE));
        when(studentRepository.existsByRun("12345678-K")).thenReturn(false);
        when(studentRepository.existsByEmail("ana@ipt.cl")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> studentService.create(
                new StudentCreateRequest("12345678-K", "Ana", "ana@ipt.cl", 1L)));

        verify(studentRepository, never()).save(any());
    }

    @Test
    void create_inactiveCareer_throwsAndDoesNotSave() {
        when(careerService.getCareerOrThrow(1L)).thenReturn(career(1L, CareerStatus.INACTIVE));
        when(studentRepository.existsByRun("12345678-K")).thenReturn(false);
        when(studentRepository.existsByEmail("ana@ipt.cl")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> studentService.create(
                new StudentCreateRequest("12345678-K", "Ana", "ana@ipt.cl", 1L)));

        verify(studentRepository, never()).save(any());
    }

    // ---------- findAll ----------

    @Test
    void findAll_withCareerAndStatus_usesCombinedFilter() {
        when(studentRepository.findByCareerIdAndStatus(1L, StudentStatus.REGULAR))
                .thenReturn(List.of(regularStudent()));

        assertEquals(1, studentService.findAll(1L, StudentStatus.REGULAR).size());
    }

    @Test
    void findAll_withCareerOnly_filtersByCareer() {
        when(studentRepository.findByCareerId(1L)).thenReturn(List.of(regularStudent()));

        assertEquals(1, studentService.findAll(1L, null).size());
    }

    @Test
    void findAll_withStatusOnly_filtersByStatus() {
        when(studentRepository.findByStatus(StudentStatus.REGULAR)).thenReturn(List.of(regularStudent()));

        assertEquals(1, studentService.findAll(null, StudentStatus.REGULAR).size());
    }

    @Test
    void findAll_withoutFilters_returnsEveryStudent() {
        when(studentRepository.findAll()).thenReturn(List.of(regularStudent(), regularStudent()));

        assertEquals(2, studentService.findAll(null, null).size());
    }

    // ---------- findById ----------

    @Test
    void findById_existing_checksAccessAndReturnsStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(regularStudent()));

        StudentResponse response = studentService.findById(1L);

        assertEquals("Ana Perez", response.fullName());
        verify(accessControlService).requireStudentAccess(1L);
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.findById(99L));
    }

    // ---------- update ----------

    @Test
    void update_basicData_keepsStatusWhenNoneIsSent() {
        Student existing = regularStudent();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("new@ipt.cl", 1L)).thenReturn(false);
        when(careerService.getCareerOrThrow(1L)).thenReturn(existing.getCareer());
        stubSave();

        StudentResponse response = studentService.update(1L, updateRequest(" NEW@ipt.cl ", 1L, null, null));

        assertEquals("New Name", response.fullName());
        assertEquals("new@ipt.cl", response.email());
        assertEquals(StudentStatus.REGULAR, response.status());
    }

    @Test
    void update_sameStatusAsCurrent_isNotAChange() {
        Student existing = regularStudent();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(careerService.getCareerOrThrow(1L)).thenReturn(existing.getCareer());
        stubSave();

        StudentResponse response = studentService.update(1L,
                updateRequest("ana@ipt.cl", 1L, null, StudentStatus.REGULAR));

        assertEquals(StudentStatus.REGULAR, response.status());
    }

    @Test
    void update_toPostponed_changesStatus() {
        Student existing = regularStudent();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(careerService.getCareerOrThrow(1L)).thenReturn(existing.getCareer());
        stubSave();

        StudentResponse response = studentService.update(1L,
                updateRequest("ana@ipt.cl", 1L, null, StudentStatus.POSTPONED));

        assertEquals(StudentStatus.POSTPONED, response.status());
    }

    @Test
    void update_toGraduatedManually_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(regularStudent()));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> studentService.update(1L,
                updateRequest("ana@ipt.cl", 1L, null, StudentStatus.GRADUATED)));

        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_toEliminatedManually_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(regularStudent()));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> studentService.update(1L,
                updateRequest("ana@ipt.cl", 1L, null, StudentStatus.ELIMINATED)));

        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_duplicatedEmail_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(regularStudent()));
        when(studentRepository.existsByEmailAndIdNot("taken@ipt.cl", 1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> studentService.update(1L,
                updateRequest("taken@ipt.cl", 1L, null, null)));

        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_planOfAnotherCareer_throws() {
        Student existing = regularStudent();
        Career otherCareer = career(2L, CareerStatus.ACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(careerService.getCareerOrThrow(1L)).thenReturn(existing.getCareer());
        when(studyPlanService.getPlanOrThrow(9L)).thenReturn(plan(9L, otherCareer));

        assertThrows(BusinessRuleException.class, () -> studentService.update(1L,
                updateRequest("ana@ipt.cl", 1L, 9L, null)));

        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_explicitPlanSameAsCurrent_isNotAPlanChange() {
        Student existing = regularStudent();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(careerService.getCareerOrThrow(1L)).thenReturn(existing.getCareer());
        when(studyPlanService.getPlanOrThrow(1L)).thenReturn(existing.getStudyPlan());
        stubSave();

        StudentResponse response = studentService.update(1L, updateRequest("ana@ipt.cl", 1L, 1L, null));

        assertEquals(1L, response.studyPlanId());
        verify(enrollmentRepository, never()).existsByStudentId(any());
    }

    @Test
    void update_differentPlanWithoutHistory_changesPlan() {
        Student existing = regularStudent();
        StudyPlan newPlan = plan(2L, existing.getCareer());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(careerService.getCareerOrThrow(1L)).thenReturn(existing.getCareer());
        when(studyPlanService.getPlanOrThrow(2L)).thenReturn(newPlan);
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        stubSave();

        StudentResponse response = studentService.update(1L, updateRequest("ana@ipt.cl", 1L, 2L, null));

        assertEquals(2L, response.studyPlanId());
    }

    @Test
    void update_differentPlanWithHistory_throws() {
        Student existing = regularStudent();
        StudyPlan newPlan = plan(2L, existing.getCareer());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(careerService.getCareerOrThrow(1L)).thenReturn(existing.getCareer());
        when(studyPlanService.getPlanOrThrow(2L)).thenReturn(newPlan);
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> studentService.update(1L,
                updateRequest("ana@ipt.cl", 1L, 2L, null)));

        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_careerChangeWithoutHistory_movesToCurrentPlanOfNewCareer() {
        Student existing = regularStudent();
        Career newCareer = career(2L, CareerStatus.ACTIVE);
        StudyPlan newCurrentPlan = plan(3L, newCareer);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(careerService.getCareerOrThrow(2L)).thenReturn(newCareer);
        when(studyPlanService.getCurrentPlanOrThrow(2L)).thenReturn(newCurrentPlan);
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);
        stubSave();

        StudentResponse response = studentService.update(1L, updateRequest("ana@ipt.cl", 2L, null, null));

        assertEquals(2L, response.careerId());
        assertEquals(3L, response.studyPlanId());
    }

    @Test
    void update_careerChangeToInactiveCareer_throws() {
        Student existing = regularStudent();
        Career inactive = career(2L, CareerStatus.INACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(careerService.getCareerOrThrow(2L)).thenReturn(inactive);
        when(studyPlanService.getCurrentPlanOrThrow(2L)).thenReturn(plan(3L, inactive));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> studentService.update(1L,
                updateRequest("ana@ipt.cl", 2L, null, null)));

        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_careerChangeWithHistory_throws() {
        Student existing = regularStudent();
        Career newCareer = career(2L, CareerStatus.ACTIVE);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.existsByEmailAndIdNot("ana@ipt.cl", 1L)).thenReturn(false);
        when(careerService.getCareerOrThrow(2L)).thenReturn(newCareer);
        when(studyPlanService.getCurrentPlanOrThrow(2L)).thenReturn(plan(3L, newCareer));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> studentService.update(1L,
                updateRequest("ana@ipt.cl", 2L, null, null)));

        verify(studentRepository, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void delete_withAcademicHistory_throws() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(regularStudent()));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> studentService.delete(1L));

        verify(studentRepository, never()).delete(any());
        verify(matriculationRepository, never()).deleteByStudentId(any());
    }

    @Test
    void delete_withoutHistory_deletesMatriculationsAndStudent() {
        Student existing = regularStudent();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.existsByStudentId(1L)).thenReturn(false);

        studentService.delete(1L);

        verify(matriculationRepository).deleteByStudentId(1L);
        verify(studentRepository).delete(existing);
    }
}