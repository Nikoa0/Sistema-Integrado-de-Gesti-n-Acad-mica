package com.ipt.siga.services;

import com.ipt.siga.dtos.TeacherCreateRequest;
import com.ipt.siga.dtos.TeacherResponse;
import com.ipt.siga.dtos.TeacherStatusRequest;
import com.ipt.siga.dtos.TeacherUpdateRequest;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.enums.TeacherStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    @Transactional
    public TeacherResponse create(TeacherCreateRequest request) {
        String run = request.run().trim().toUpperCase();
        String email = request.email().trim().toLowerCase();

        if (teacherRepository.existsByRun(run)) {
            throw new BusinessRuleException("A teacher with RUN " + run + " already exists");
        }
        if (teacherRepository.existsByEmail(email)) {
            throw new BusinessRuleException("A teacher with email " + email + " already exists");
        }

        Teacher teacher = new Teacher();
        teacher.setRun(run);
        teacher.setFullName(request.fullName().trim());
        teacher.setEmail(email);
        teacher.setProfessionalTitle(request.professionalTitle().trim());
        teacher.setAcademicDegree(request.academicDegree().trim());
        teacher.setStatus(TeacherStatus.ACTIVE);
        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll(TeacherStatus status) {
        List<Teacher> teachers = (status == null)
                ? teacherRepository.findAll()
                : teacherRepository.findByStatus(status);
        return teachers.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(Long id) {
        return toResponse(getTeacherOrThrow(id));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherUpdateRequest request) {
        Teacher teacher = getTeacherOrThrow(id);
        String email = request.email().trim().toLowerCase();

        if (teacherRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessRuleException("A teacher with email " + email + " already exists");
        }

        teacher.setFullName(request.fullName().trim());
        teacher.setEmail(email);
        teacher.setProfessionalTitle(request.professionalTitle().trim());
        teacher.setAcademicDegree(request.academicDegree().trim());
        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse updateStatus(Long id, TeacherStatusRequest request) {
        Teacher teacher = getTeacherOrThrow(id);

        if (request.status() == TeacherStatus.INACTIVE && hasSectionsInOpenPeriod(teacher)) {
            throw new BusinessRuleException(
                    "Teacher has sections in an OPEN academic period and cannot be set to INACTIVE");
        }
        teacher.setStatus(request.status());
        return toResponse(teacherRepository.save(teacher));
    }

    @Transactional
    public void delete(Long id) {
        Teacher teacher = getTeacherOrThrow(id);
        if (hasSections(teacher)) {
            throw new BusinessRuleException("Teacher has sections and cannot be deleted");
        }
        teacherRepository.delete(teacher);
    }

    public Teacher getTeacherOrThrow(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id " + id));
    }

    // TODO (Epic 5): return true if the teacher has any section assigned
    private boolean hasSections(Teacher teacher) {
        return false;
    }

    // TODO (Epic 5): return true if the teacher has sections in an OPEN academic period
    private boolean hasSectionsInOpenPeriod(Teacher teacher) {
        return false;
    }

    private TeacherResponse toResponse(Teacher t) {
        return new TeacherResponse(t.getId(), t.getRun(), t.getFullName(), t.getEmail(),
                t.getProfessionalTitle(), t.getAcademicDegree(), t.getStatus());
    }
}