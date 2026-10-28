package com.ipt.siga.services;

import com.ipt.siga.dtos.SectionCreateRequest;
import com.ipt.siga.dtos.SectionUpdateRequest;
import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.ScheduleBlock;
import com.ipt.siga.entities.Section;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.enums.TeacherStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.EnrollmentRepository;
import com.ipt.siga.repositories.SectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private AcademicPeriodService academicPeriodService;
    @Mock
    private SubjectService subjectService;
    @Mock
    private TeacherService teacherService;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private SectionService sectionService;

    // ---------- helpers ----------

    private AcademicPeriod period(PeriodStatus status) {
        AcademicPeriod p = new AcademicPeriod();
        p.setId(1L);
        p.setAcademicYear(2028);
        p.setSemester(1);
        p.setStatus(status);
        return p;
    }

    private Teacher teacher(Long id, TeacherStatus status) {
        Teacher t = new Teacher();
        t.setId(id);
        t.setFullName("Teacher " + id);
        t.setStatus(status);
        return t;
    }

    private Subject subject(Long id, String code, int theory, int exercise, int lab) {
        Subject s = new Subject();
        s.setId(id);
        s.setCode(code);
        s.setName("Name " + code);
        s.setTheoryHours(theory);
        s.setExerciseHours(exercise);
        s.setLabHours(lab);
        return s;
    }

    // 4 weekly hours = 2 blocks
    private Subject twoBlockSubject() {
        return subject(2L, "TAP102", 2, 2, 0);
    }

    private Set<ScheduleBlock> blocks(String... codes) {
        Set<ScheduleBlock> result = new HashSet<>();
        for (String code : codes) {
            result.add(ScheduleBlock.fromCode(code));
        }
        return result;
    }

    private Section section(Long id, AcademicPeriod period, Subject subject, Teacher teacher,
                            int capacity, String... codes) {
        Section s = new Section();
        s.setId(id);
        s.setPeriod(period);
        s.setSubject(subject);
        s.setTeacher(teacher);
        s.setCapacity(capacity);
        s.setBlocks(blocks(codes));
        return s;
    }

    private SectionCreateRequest createRequest(Long subjectId, Long teacherId, int capacity, String... codes) {
        return new SectionCreateRequest(subjectId, teacherId, capacity, List.of(codes));
    }

    // Stubs everything create() needs up to (and including) loading the teacher
    private void stubCreateUntilTeacher(AcademicPeriod period, Subject subject, Teacher teacher) {
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period);
        when(subjectService.getSubjectOrThrow(subject.getId())).thenReturn(subject);
        when(sectionRepository.existsByPeriodIdAndSubjectId(1L, subject.getId())).thenReturn(false);
        when(teacherService.getTeacherOrThrow(teacher.getId())).thenReturn(teacher);
    }

    // ---------- create ----------

    @Test
    void create_validRequest_savesSectionWithItsBlocks() {
        AcademicPeriod period = period(PeriodStatus.OPEN);
        Subject subject = twoBlockSubject();
        Teacher teacher = teacher(7L, TeacherStatus.ACTIVE);
        stubCreateUntilTeacher(period, subject, teacher);
        // The teacher already teaches another section, at different blocks
        Section other = section(50L, period, subject(3L, "TAP103", 2, 0, 2), teacher, 30, "W1", "W2");
        when(sectionRepository.findByPeriodIdAndTeacherId(1L, 7L)).thenReturn(List.of(other));
        when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> {
            Section s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        assertNotNull(sectionService.create(1L, createRequest(2L, 7L, 30, "M2", "M1")));

        ArgumentCaptor<Section> captor = ArgumentCaptor.forClass(Section.class);
        verify(sectionRepository).save(captor.capture());
        Section saved = captor.getValue();
        assertEquals(30, saved.getCapacity());
        assertSame(teacher, saved.getTeacher());
        assertEquals(List.of("M1", "M2"),
                saved.getBlocks().stream().map(ScheduleBlock::toCode).sorted().toList());
    }

    @Test
    void create_closedPeriod_throws() {
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period(PeriodStatus.CLOSED));

        assertThrows(BusinessRuleException.class,
                () -> sectionService.create(1L, createRequest(2L, 7L, 30, "M1", "M2")));

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void create_subjectAlreadyHasASectionInThePeriod_throws() {
        Subject subject = twoBlockSubject();
        when(academicPeriodService.getPeriodOrThrow(1L)).thenReturn(period(PeriodStatus.OPEN));
        when(subjectService.getSubjectOrThrow(2L)).thenReturn(subject);
        when(sectionRepository.existsByPeriodIdAndSubjectId(1L, 2L)).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> sectionService.create(1L, createRequest(2L, 7L, 30, "M1", "M2")));

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void create_inactiveTeacher_throws() {
        stubCreateUntilTeacher(period(PeriodStatus.OPEN), twoBlockSubject(), teacher(7L, TeacherStatus.INACTIVE));

        assertThrows(BusinessRuleException.class,
                () -> sectionService.create(1L, createRequest(2L, 7L, 30, "M1", "M2")));

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void create_duplicatedBlock_throws() {
        stubCreateUntilTeacher(period(PeriodStatus.OPEN), twoBlockSubject(), teacher(7L, TeacherStatus.ACTIVE));

        assertThrows(BusinessRuleException.class,
                () -> sectionService.create(1L, createRequest(2L, 7L, 30, "M1", "M1")));

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void create_weeklyHoursNotDivisibleIntoBlocks_throws() {
        Subject oddHours = subject(2L, "TAPX", 2, 1, 0); // 3 hours
        stubCreateUntilTeacher(period(PeriodStatus.OPEN), oddHours, teacher(7L, TeacherStatus.ACTIVE));

        assertThrows(BusinessRuleException.class,
                () -> sectionService.create(1L, createRequest(2L, 7L, 30, "M1", "M2")));

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void create_wrongNumberOfBlocks_throws() {
        stubCreateUntilTeacher(period(PeriodStatus.OPEN), twoBlockSubject(), teacher(7L, TeacherStatus.ACTIVE));

        assertThrows(BusinessRuleException.class,
                () -> sectionService.create(1L, createRequest(2L, 7L, 30, "M1"))); // needs 2

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void create_teacherAlreadyTeachesAtThoseBlocks_throws() {
        AcademicPeriod period = period(PeriodStatus.OPEN);
        Teacher teacher = teacher(7L, TeacherStatus.ACTIVE);
        stubCreateUntilTeacher(period, twoBlockSubject(), teacher);
        Section other = section(50L, period, subject(3L, "TAP103", 2, 0, 2), teacher, 30, "M1", "J1");
        when(sectionRepository.findByPeriodIdAndTeacherId(1L, 7L)).thenReturn(List.of(other));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> sectionService.create(1L, createRequest(2L, 7L, 30, "M1", "M2")));

        assertTrue(ex.getMessage().contains("M1"));
        verify(sectionRepository, never()).save(any());
    }

    // ---------- findByPeriod ----------

    @Test
    void findByPeriod_admin_withoutFilter_returnsEverySection() {
        AcademicPeriod period = period(PeriodStatus.OPEN);
        Teacher teacher = teacher(7L, TeacherStatus.ACTIVE);
        when(sectionRepository.findByPeriodId(1L)).thenReturn(List.of(
                section(1L, period, twoBlockSubject(), teacher, 30, "M1", "M2")));

        assertEquals(1, sectionService.findByPeriod(1L, null).size());

        verify(academicPeriodService).getPeriodOrThrow(1L);
    }

    @Test
    void findByPeriod_admin_withTeacherFilter_filtersByThatTeacher() {
        AcademicPeriod period = period(PeriodStatus.OPEN);
        Teacher teacher = teacher(5L, TeacherStatus.ACTIVE);
        when(sectionRepository.findByPeriodIdAndTeacherId(1L, 5L)).thenReturn(List.of(
                section(1L, period, twoBlockSubject(), teacher, 30, "M1", "M2")));

        assertEquals(1, sectionService.findByPeriod(1L, 5L).size());
    }

    @Test
    void findByPeriod_teacher_alwaysSeesOnlyTheirOwnSections() {
        AcademicPeriod period = period(PeriodStatus.OPEN);
        Teacher me = teacher(7L, TeacherStatus.ACTIVE);
        when(accessControlService.hasRole("TEACHER")).thenReturn(true);
        when(accessControlService.hasRole("ADMIN")).thenReturn(false);
        when(accessControlService.currentTeacher()).thenReturn(me);
        when(sectionRepository.findByPeriodIdAndTeacherId(1L, 7L)).thenReturn(List.of(
                section(1L, period, twoBlockSubject(), me, 30, "M1", "M2")));

        // The teacher tries to look at teacher 99's sections: the filter is ignored
        assertEquals(1, sectionService.findByPeriod(1L, 99L).size());

        verify(sectionRepository, never()).findByPeriodIdAndTeacherId(1L, 99L);
    }

    @Test
    void findByPeriod_userWithTeacherAndAdminRoles_keepsTheRequestedFilter() {
        when(accessControlService.hasRole("TEACHER")).thenReturn(true);
        when(accessControlService.hasRole("ADMIN")).thenReturn(true);
        when(sectionRepository.findByPeriodIdAndTeacherId(1L, 99L)).thenReturn(List.of());

        assertTrue(sectionService.findByPeriod(1L, 99L).isEmpty());

        verify(accessControlService, never()).currentTeacher();
    }

    // ---------- findById / findOffer ----------

    @Test
    void findById_existing_returnsSection() {
        Section s = section(11L, period(PeriodStatus.OPEN), twoBlockSubject(),
                teacher(7L, TeacherStatus.ACTIVE), 30, "M1", "M2");
        when(sectionRepository.findById(11L)).thenReturn(Optional.of(s));

        assertNotNull(sectionService.findById(11L));
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sectionService.findById(99L));
    }

    @Test
    void findOffer_returnsTheSectionsOfTheStudyPlan() {
        Section s = section(11L, period(PeriodStatus.OPEN), twoBlockSubject(),
                teacher(7L, TeacherStatus.ACTIVE), 30, "M1", "M2");
        when(sectionRepository.findByPeriodIdAndSubjectStudyPlanId(1L, 5L)).thenReturn(List.of(s));

        assertEquals(1, sectionService.findOffer(1L, 5L).size());
    }

    // ---------- update ----------

    private Section existingSection(Teacher teacher) {
        return section(11L, period(PeriodStatus.OPEN), twoBlockSubject(), teacher, 30, "M1", "M2");
    }

    @Test
    void update_sameTeacher_changesCapacityAndBlocks() {
        Teacher teacher = teacher(7L, TeacherStatus.ACTIVE);
        Section existing = existingSection(teacher);
        when(sectionRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(teacherService.getTeacherOrThrow(7L)).thenReturn(teacher);
        // The teacher's only section is this same one: it must not conflict with itself
        when(sectionRepository.findByPeriodIdAndTeacherId(1L, 7L)).thenReturn(List.of(existing));
        when(enrollmentRepository.countBySectionId(11L)).thenReturn(10L);
        when(sectionRepository.save(existing)).thenReturn(existing);

        sectionService.update(11L, new SectionUpdateRequest(7L, 40, List.of("J1", "J2")));

        assertEquals(40, existing.getCapacity());
        assertEquals(List.of("J1", "J2"),
                existing.getBlocks().stream().map(ScheduleBlock::toCode).sorted().toList());
    }

    @Test
    void update_differentActiveTeacher_assignsHim() {
        Section existing = existingSection(teacher(7L, TeacherStatus.ACTIVE));
        Teacher newTeacher = teacher(8L, TeacherStatus.ACTIVE);
        when(sectionRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(teacherService.getTeacherOrThrow(8L)).thenReturn(newTeacher);
        when(sectionRepository.findByPeriodIdAndTeacherId(1L, 8L)).thenReturn(List.of());
        when(enrollmentRepository.countBySectionId(11L)).thenReturn(0L);
        when(sectionRepository.save(existing)).thenReturn(existing);

        sectionService.update(11L, new SectionUpdateRequest(8L, 30, List.of("M1", "M2")));

        assertSame(newTeacher, existing.getTeacher());
    }

    @Test
    void update_differentInactiveTeacher_throws() {
        Section existing = existingSection(teacher(7L, TeacherStatus.ACTIVE));
        when(sectionRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(teacherService.getTeacherOrThrow(8L)).thenReturn(teacher(8L, TeacherStatus.INACTIVE));

        assertThrows(BusinessRuleException.class,
                () -> sectionService.update(11L, new SectionUpdateRequest(8L, 30, List.of("M1", "M2"))));

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void update_closedPeriod_throws() {
        Section existing = existingSection(teacher(7L, TeacherStatus.ACTIVE));
        existing.getPeriod().setStatus(PeriodStatus.CLOSED);
        when(sectionRepository.findById(11L)).thenReturn(Optional.of(existing));

        assertThrows(BusinessRuleException.class,
                () -> sectionService.update(11L, new SectionUpdateRequest(7L, 30, List.of("M1", "M2"))));

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void update_capacityLowerThanEnrolledStudents_throws() {
        Teacher teacher = teacher(7L, TeacherStatus.ACTIVE);
        Section existing = existingSection(teacher);
        when(sectionRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(teacherService.getTeacherOrThrow(7L)).thenReturn(teacher);
        when(sectionRepository.findByPeriodIdAndTeacherId(1L, 7L)).thenReturn(List.of());
        when(enrollmentRepository.countBySectionId(11L)).thenReturn(20L);

        assertThrows(BusinessRuleException.class,
                () -> sectionService.update(11L, new SectionUpdateRequest(7L, 10, List.of("M1", "M2"))));

        verify(sectionRepository, never()).save(any());
    }

    @Test
    void update_missingSection_throwsNotFound() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> sectionService.update(99L, new SectionUpdateRequest(7L, 30, List.of("M1", "M2"))));
    }

    // ---------- delete ----------

    @Test
    void delete_closedPeriod_throws() {
        Section existing = existingSection(teacher(7L, TeacherStatus.ACTIVE));
        existing.getPeriod().setStatus(PeriodStatus.CLOSED);
        when(sectionRepository.findById(11L)).thenReturn(Optional.of(existing));

        assertThrows(BusinessRuleException.class, () -> sectionService.delete(11L));

        verify(sectionRepository, never()).delete(any());
    }

    @Test
    void delete_withEnrolledStudents_throws() {
        Section existing = existingSection(teacher(7L, TeacherStatus.ACTIVE));
        when(sectionRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.existsBySectionId(11L)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> sectionService.delete(11L));

        verify(sectionRepository, never()).delete(any());
    }

    @Test
    void delete_withoutEnrollments_deletesSection() {
        Section existing = existingSection(teacher(7L, TeacherStatus.ACTIVE));
        when(sectionRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(enrollmentRepository.existsBySectionId(11L)).thenReturn(false);

        sectionService.delete(11L);

        verify(sectionRepository).delete(existing);
    }
}