package com.ipt.siga.services;

import com.ipt.siga.dtos.MatriculationRequest;
import com.ipt.siga.dtos.MatriculationResponse;
import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Matriculation;
import com.ipt.siga.entities.Student;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.EnrollmentRepository;
import com.ipt.siga.repositories.MatriculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatriculationService {

    private final MatriculationRepository matriculationRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final AcademicPeriodService academicPeriodService;

    @Transactional
    public MatriculationResponse create(Long periodId, MatriculationRequest request) {
        AcademicPeriod period = academicPeriodService.getPeriodOrThrow(periodId);
        Student student = studentService.getStudentOrThrow(request.studentId());

        if (matriculationRepository.existsByStudentIdAndPeriodId(student.getId(), periodId)) {
            throw new BusinessRuleException("The student is already matriculated in this academic period");
        }

        Matriculation matriculation = new Matriculation();
        matriculation.setStudent(student);
        matriculation.setPeriod(period);
        return toResponse(matriculationRepository.save(matriculation));
    }

    @Transactional(readOnly = true)
    public List<MatriculationResponse> findByPeriod(Long periodId) {
        academicPeriodService.getPeriodOrThrow(periodId);
        return matriculationRepository.findByPeriodId(periodId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        Matriculation matriculation = matriculationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matriculation not found with id " + id));
        if (enrollmentRepository.existsByStudentIdAndPeriodId(
                matriculation.getStudent().getId(), matriculation.getPeriod().getId())) {
            throw new BusinessRuleException("The student has enrollments in this period, so the matriculation cannot be removed");
        }
        matriculationRepository.delete(matriculation);
    }

    private MatriculationResponse toResponse(Matriculation m) {
        return new MatriculationResponse(m.getId(), m.getStudent().getId(), m.getStudent().getFullName(),
                m.getPeriod().getId(), m.getPeriod().getAcademicYear() + "-" + m.getPeriod().getSemester());
    }
}