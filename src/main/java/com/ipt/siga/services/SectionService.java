package com.ipt.siga.services;

import com.ipt.siga.dtos.SectionCreateRequest;
import com.ipt.siga.dtos.SectionResponse;
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
import com.ipt.siga.repositories.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ipt.siga.repositories.EnrollmentRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SectionService {

    private static final int HOURS_PER_BLOCK = 2;

    private final SectionRepository sectionRepository;
    private final AcademicPeriodService academicPeriodService;
    private final SubjectService subjectService;
    private final TeacherService teacherService;
    private final EnrollmentRepository enrollmentRepository;
    private final AccessControlService accessControlService; // NEW

    @Transactional
    public SectionResponse create(Long periodId, SectionCreateRequest request) {
        AcademicPeriod period = academicPeriodService.getPeriodOrThrow(periodId);
        requireOpen(period);

        Subject subject = subjectService.getSubjectOrThrow(request.subjectId());
        if (sectionRepository.existsByPeriodIdAndSubjectId(periodId, subject.getId())) {
            throw new BusinessRuleException("Subject " + subject.getCode()
                    + " already has a section in this academic period");
        }

        Teacher teacher = teacherService.getTeacherOrThrow(request.teacherId());
        requireActiveTeacher(teacher);

        Set<ScheduleBlock> blocks = parseBlocks(subject, request.blocks());
        checkTeacherConflicts(periodId, teacher.getId(), blocks, null);

        Section section = new Section();
        section.setPeriod(period);
        section.setSubject(subject);
        section.setTeacher(teacher);
        section.setCapacity(request.capacity());
        section.setBlocks(blocks);
        return toResponse(sectionRepository.save(section));
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> findByPeriod(Long periodId, Long teacherId) {
        academicPeriodService.getPeriodOrThrow(periodId);

        // NEW: a TEACHER only sees their own sections, even if they send another teacherId
        Long filterTeacherId = teacherId;
        if (accessControlService.hasRole("TEACHER") && !accessControlService.hasRole("ADMIN")) {
            filterTeacherId = accessControlService.currentTeacher().getId();
        }

        List<Section> sections = (filterTeacherId == null)
                ? sectionRepository.findByPeriodId(periodId)
                : sectionRepository.findByPeriodIdAndTeacherId(periodId, filterTeacherId);
        return sections.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SectionResponse findById(Long id) {
        return toResponse(getSectionOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> findOffer(Long periodId, Long studyPlanId) {
        return sectionRepository.findByPeriodIdAndSubjectStudyPlanId(periodId, studyPlanId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public SectionResponse update(Long id, SectionUpdateRequest request) {
        Section section = getSectionOrThrow(id);
        AcademicPeriod period = section.getPeriod();
        requireOpen(period);

        Teacher teacher = teacherService.getTeacherOrThrow(request.teacherId());
        if (!teacher.getId().equals(section.getTeacher().getId())) {
            requireActiveTeacher(teacher);
        }

        Set<ScheduleBlock> blocks = parseBlocks(section.getSubject(), request.blocks());
        checkTeacherConflicts(period.getId(), teacher.getId(), blocks, id);
        if (enrollmentRepository.countBySectionId(id) > request.capacity()) {
            throw new BusinessRuleException("Capacity cannot be lower than the number of enrolled students");
        }
        section.setTeacher(teacher);
        section.setCapacity(request.capacity());
        section.getBlocks().clear();
        section.getBlocks().addAll(blocks);
        return toResponse(sectionRepository.save(section));
    }

    @Transactional
    public void delete(Long id) {
        Section section = getSectionOrThrow(id);
        requireOpen(section.getPeriod());
        if (enrollmentRepository.existsBySectionId(id)) {
            throw new BusinessRuleException("Section has enrolled students and cannot be deleted");
        }
        sectionRepository.delete(section);
    }

    public Section getSectionOrThrow(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found with id " + id));
    }

    private void requireOpen(AcademicPeriod period) {
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("The academic period is CLOSED and its offer cannot be modified");
        }
    }

    private void requireActiveTeacher(Teacher teacher) {
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE teachers can be assigned to a section");
        }
    }

    // Converts "L2", "W3"... into blocks and checks duplicates and the number of blocks
    private Set<ScheduleBlock> parseBlocks(Subject subject, List<String> codes) {
        Set<ScheduleBlock> blocks = new HashSet<>();
        for (String code : codes) {
            if (!blocks.add(ScheduleBlock.fromCode(code))) {
                throw new BusinessRuleException("Duplicate time block " + code);
            }
        }

        int weeklyHours = subject.getTheoryHours() + subject.getExerciseHours() + subject.getLabHours();
        if (weeklyHours % HOURS_PER_BLOCK != 0) {
            throw new BusinessRuleException("Subject " + subject.getCode() + " has " + weeklyHours
                    + " weekly hours, which cannot be divided into blocks of " + HOURS_PER_BLOCK + " hours");
        }
        int expectedBlocks = weeklyHours / HOURS_PER_BLOCK;
        if (blocks.size() != expectedBlocks) {
            throw new BusinessRuleException("Subject " + subject.getCode() + " requires " + expectedBlocks
                    + " time blocks (" + weeklyHours + " weekly hours / " + HOURS_PER_BLOCK
                    + "), but " + blocks.size() + " were received");
        }
        return blocks;
    }

    // A teacher cannot have two sections of the same period on the same day and module
    private void checkTeacherConflicts(Long periodId, Long teacherId, Set<ScheduleBlock> blocks,
                                       Long excludedSectionId) {
        for (Section other : sectionRepository.findByPeriodIdAndTeacherId(periodId, teacherId)) {
            if (other.getId().equals(excludedSectionId)) {
                continue;
            }
            for (ScheduleBlock block : other.getBlocks()) {
                if (blocks.contains(block)) {
                    throw new BusinessRuleException("Teacher already teaches "
                            + other.getSubject().getCode() + " at block " + block.toCode());
                }
            }
        }
    }

    private SectionResponse toResponse(Section s) {
        List<String> blockCodes = s.getBlocks().stream()
                .sorted(Comparator.comparing(ScheduleBlock::getDay)
                        .thenComparing(ScheduleBlock::getModuleNumber))
                .map(ScheduleBlock::toCode)
                .toList();
        return new SectionResponse(
                s.getId(),
                s.getPeriod().getId(),
                s.getPeriod().getAcademicYear() + "-" + s.getPeriod().getSemester(),
                s.getSubject().getId(), s.getSubject().getCode(), s.getSubject().getName(),
                s.getTeacher().getId(), s.getTeacher().getFullName(),
                s.getCapacity(), blockCodes);
    }
}