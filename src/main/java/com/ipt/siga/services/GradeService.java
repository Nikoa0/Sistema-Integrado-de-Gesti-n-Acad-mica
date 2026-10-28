package com.ipt.siga.services;

import com.ipt.siga.dtos.EnrollmentResponse;
import com.ipt.siga.dtos.GradeRequest;
import com.ipt.siga.entities.Enrollment;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.exceptions.BusinessRuleException;
import com.ipt.siga.exceptions.ForbiddenException;
import com.ipt.siga.exceptions.ResourceNotFoundException;
import com.ipt.siga.repositories.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final EnrollmentRepository enrollmentRepository;
    private final AccessControlService accessControlService;
    private final EnrollmentService enrollmentService;

    @Transactional
    public EnrollmentResponse register(Long enrollmentId, GradeRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id " + enrollmentId));

        // The teacher comes from the token, not from the request body
        Teacher teacher = accessControlService.currentTeacher();
        if (!enrollment.getSection().getTeacher().getId().equals(teacher.getId())) {
            throw new ForbiddenException("Only the teacher responsible for the section can register its grades");
        }

        if (enrollment.getPeriod().getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("The academic period is CLOSED: grades cannot be registered or modified");
        }

        enrollment.setGrade(request.grade());
        return enrollmentService.toResponse(enrollmentRepository.save(enrollment));
    }
}