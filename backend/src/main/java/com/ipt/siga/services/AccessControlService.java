package com.ipt.siga.services;

import com.ipt.siga.dtos.MeResponse;
import com.ipt.siga.entities.Section;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.exceptions.ForbiddenException;
import com.ipt.siga.repositories.StudentRepository;
import com.ipt.siga.repositories.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

// Answers "who is making this request?" using the JWT issued by Keycloak
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    public String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email.trim().toLowerCase();
            }
        }
        throw new ForbiddenException("The access token has no email");
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    public Teacher currentTeacher() {
        return teacherRepository.findByEmail(currentEmail())
                .orElseThrow(() -> new ForbiddenException("No teacher is linked to this user"));
    }

    public Student currentStudent() {
        return studentRepository.findByEmail(currentEmail())
                .orElseThrow(() -> new ForbiddenException("No student is linked to this user"));
    }

    // ADMIN can access any student; a STUDENT only their own data
    public void requireStudentAccess(Long studentId) {
        if (hasRole("ADMIN")) {
            return;
        }
        if (!currentStudent().getId().equals(studentId)) {
            throw new ForbiddenException("You can only access your own academic information");
        }
    }

    // ADMIN can access any section; a TEACHER only the ones assigned to them
    public void requireSectionAccess(Section section) {
        if (hasRole("ADMIN")) {
            return;
        }
        if (!currentTeacher().getId().equals(section.getTeacher().getId())) {
            throw new ForbiddenException("You can only access the sections assigned to you");
        }
    }

    public MeResponse me() {
        String email = currentEmail();
        List<String> roles = List.of("ADMIN", "TEACHER", "STUDENT").stream()
                .filter(this::hasRole).toList();
        Long teacherId = teacherRepository.findByEmail(email).map(Teacher::getId).orElse(null);
        Long studentId = studentRepository.findByEmail(email).map(Student::getId).orElse(null);
        return new MeResponse(email, roles, teacherId, studentId);
    }
}