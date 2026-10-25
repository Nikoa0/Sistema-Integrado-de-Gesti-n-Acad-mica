package com.ipt.siga.services;

import com.ipt.siga.dtos.StudentCreateRequest;
import com.ipt.siga.dtos.StudentResponse;
import com.ipt.siga.dtos.StudentStatusRequest;
import com.ipt.siga.dtos.StudentUpdateRequest;
import com.ipt.siga.entities.Career;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.enums.CareerStatus;
import com.ipt.siga.enums.StudentStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final CareerService careerService;
    private final StudyPlanService studyPlanService;

    @Transactional
    public StudentResponse create(StudentCreateRequest request) {
        Career career = careerService.getCareerOrThrow(request.careerId());
        String run = request.run().trim().toUpperCase();
        String email = request.email().trim().toLowerCase();

        if (studentRepository.existsByRun(run)) {
            throw new BusinessRuleException("A student with RUN " + run + " already exists");
        }
        if (studentRepository.existsByEmail(email)) {
            throw new BusinessRuleException("A student with email " + email + " already exists");
        }
        requireActiveCareer(career);

        Student student = new Student();
        student.setRun(run);
        student.setFullName(request.fullName().trim());
        student.setEmail(email);
        student.setCareer(career);
        student.setStudyPlan(studyPlanService.getCurrentPlanOrThrow(career.getId()));
        student.setStatus(StudentStatus.REGULAR);
        return toResponse(studentRepository.save(student));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll(Long careerId, StudentStatus status) {
        List<Student> students;
        if (careerId != null && status != null) {
            students = studentRepository.findByCareerIdAndStatus(careerId, status);
        } else if (careerId != null) {
            students = studentRepository.findByCareerId(careerId);
        } else if (status != null) {
            students = studentRepository.findByStatus(status);
        } else {
            students = studentRepository.findAll();
        }
        return students.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        return toResponse(getStudentOrThrow(id));
    }

    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest request) {
        Student student = getStudentOrThrow(id);
        String email = request.email().trim().toLowerCase();

        if (studentRepository.existsByEmailAndIdNot(email, id)) {
            throw new BusinessRuleException("A student with email " + email + " already exists");
        }

        Career career = careerService.getCareerOrThrow(request.careerId());
        boolean careerChanged = !student.getCareer().getId().equals(career.getId());

        StudyPlan newPlan = student.getStudyPlan();
        if (request.studyPlanId() != null) {
            newPlan = studyPlanService.getPlanOrThrow(request.studyPlanId());
            if (!newPlan.getCareer().getId().equals(career.getId())) {
                throw new BusinessRuleException("The study plan does not belong to the selected career");
            }
        } else if (careerChanged) {
            newPlan = studyPlanService.getCurrentPlanOrThrow(career.getId());
        }
        boolean planChanged = !newPlan.getId().equals(student.getStudyPlan().getId());

        if (careerChanged || planChanged) {
            if (hasAcademicHistory(student)) {
                throw new BusinessRuleException(
                        "Career and study plan cannot be changed once the student has enrollments or academic history");
            }
            if (careerChanged) {
                requireActiveCareer(career);
            }
        }

        student.setFullName(request.fullName().trim());
        student.setEmail(email);
        student.setCareer(career);
        student.setStudyPlan(newPlan);
        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse updateStatus(Long id, StudentStatusRequest request) {
        Student student = getStudentOrThrow(id);
        StudentStatus target = request.status();

        if (target == StudentStatus.GRADUATED || target == StudentStatus.ELIMINATED) {
            throw new BusinessRuleException(
                    "GRADUATED and ELIMINATED are assigned automatically when an academic period is closed");
        }
        if (student.getStatus() == StudentStatus.GRADUATED || student.getStatus() == StudentStatus.ELIMINATED) {
            throw new BusinessRuleException(
                    "The status of a graduated or eliminated student cannot be changed");
        }
        student.setStatus(target);
        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public void delete(Long id) {
        Student student = getStudentOrThrow(id);
        if (hasAcademicHistory(student)) {
            throw new BusinessRuleException(
                    "Student has enrollments or academic history and cannot be deleted");
        }
        studentRepository.delete(student);
    }

    public Student getStudentOrThrow(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id " + id));
    }

    private void requireActiveCareer(Career career) {
        if (career.getStatus() != CareerStatus.ACTIVE) {
            throw new BusinessRuleException("An INACTIVE career cannot receive new students");
        }
    }

    // TODO (Epics 6 and 7): return true if the student has enrollments or academic history
    private boolean hasAcademicHistory(Student student) {
        return false;
    }

    private StudentResponse toResponse(Student s) {
        return new StudentResponse(s.getId(), s.getRun(), s.getFullName(), s.getEmail(),
                s.getCareer().getId(), s.getCareer().getName(),
                s.getStudyPlan().getId(), s.getStudyPlan().getIdentifier(), s.getStatus());
    }
}