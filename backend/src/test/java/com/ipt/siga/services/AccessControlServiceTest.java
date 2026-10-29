package com.ipt.siga.services;

import com.ipt.siga.dtos.MeResponse;
import com.ipt.siga.entities.Section;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.exceptions.ForbiddenException;
import com.ipt.siga.repositories.StudentRepository;
import com.ipt.siga.repositories.TeacherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private AccessControlService accessControlService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- helpers ----------

    // Puts a fake Keycloak JWT (with the given email and roles) in the security context
    private void login(String email, String... roles) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("keycloak-user-id");
        if (email != null) {
            builder.claim("email", email);
        }
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(builder.build(), authorities));
    }

    private Teacher teacher(Long id) {
        Teacher t = new Teacher();
        t.setId(id);
        return t;
    }

    private Student student(Long id) {
        Student s = new Student();
        s.setId(id);
        return s;
    }

    private Section sectionOf(Long teacherId) {
        Section s = new Section();
        s.setTeacher(teacher(teacherId));
        return s;
    }

    // ---------- currentEmail ----------

    @Test
    void currentEmail_returnsTrimmedLowercaseClaim() {
        login(" Ana@IPT.cl ", "STUDENT");

        assertEquals("ana@ipt.cl", accessControlService.currentEmail());
    }

    @Test
    void currentEmail_withoutAuthentication_throwsForbidden() {
        assertThrows(ForbiddenException.class, () -> accessControlService.currentEmail());
    }

    @Test
    void currentEmail_principalIsNotAJwt_throwsForbidden() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "password", List.of()));

        assertThrows(ForbiddenException.class, () -> accessControlService.currentEmail());
    }

    @Test
    void currentEmail_tokenWithoutEmailClaim_throwsForbidden() {
        login(null, "STUDENT");

        assertThrows(ForbiddenException.class, () -> accessControlService.currentEmail());
    }

    @Test
    void currentEmail_blankEmail_throwsForbidden() {
        login("   ", "STUDENT");

        assertThrows(ForbiddenException.class, () -> accessControlService.currentEmail());
    }

    // ---------- hasRole ----------

    @Test
    void hasRole_withoutAuthentication_isFalse() {
        assertFalse(accessControlService.hasRole("ADMIN"));
    }

    @Test
    void hasRole_matchesOnlyTheGrantedRoles() {
        login("docente@ipt.cl", "TEACHER");

        assertTrue(accessControlService.hasRole("TEACHER"));
        assertFalse(accessControlService.hasRole("ADMIN"));
    }

    // ---------- currentTeacher / currentStudent ----------

    @Test
    void currentTeacher_linkedByEmail_returnsTeacher() {
        login("docente@ipt.cl", "TEACHER");
        Teacher teacher = teacher(1L);
        when(teacherRepository.findByEmail("docente@ipt.cl")).thenReturn(Optional.of(teacher));

        assertSame(teacher, accessControlService.currentTeacher());
    }

    @Test
    void currentTeacher_notLinked_throwsForbidden() {
        login("docente@ipt.cl", "TEACHER");
        when(teacherRepository.findByEmail("docente@ipt.cl")).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> accessControlService.currentTeacher());
    }

    @Test
    void currentStudent_linkedByEmail_returnsStudent() {
        login("ana@ipt.cl", "STUDENT");
        Student student = student(5L);
        when(studentRepository.findByEmail("ana@ipt.cl")).thenReturn(Optional.of(student));

        assertSame(student, accessControlService.currentStudent());
    }

    @Test
    void currentStudent_notLinked_throwsForbidden() {
        login("ana@ipt.cl", "STUDENT");
        when(studentRepository.findByEmail("ana@ipt.cl")).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> accessControlService.currentStudent());
    }

    // ---------- requireStudentAccess ----------

    @Test
    void requireStudentAccess_admin_passesWithoutLookingUpAStudent() {
        login("admin@ipt.cl", "ADMIN");

        assertDoesNotThrow(() -> accessControlService.requireStudentAccess(99L));

        verifyNoInteractions(studentRepository);
    }

    @Test
    void requireStudentAccess_ownData_passes() {
        login("ana@ipt.cl", "STUDENT");
        when(studentRepository.findByEmail("ana@ipt.cl")).thenReturn(Optional.of(student(5L)));

        assertDoesNotThrow(() -> accessControlService.requireStudentAccess(5L));
    }

    @Test
    void requireStudentAccess_anotherStudent_throwsForbidden() {
        login("ana@ipt.cl", "STUDENT");
        when(studentRepository.findByEmail("ana@ipt.cl")).thenReturn(Optional.of(student(5L)));

        assertThrows(ForbiddenException.class, () -> accessControlService.requireStudentAccess(6L));
    }

    // ---------- requireSectionAccess ----------

    @Test
    void requireSectionAccess_admin_passesWithoutLookingUpATeacher() {
        login("admin@ipt.cl", "ADMIN");

        assertDoesNotThrow(() -> accessControlService.requireSectionAccess(sectionOf(1L)));

        verifyNoInteractions(teacherRepository);
    }

    @Test
    void requireSectionAccess_assignedTeacher_passes() {
        login("docente@ipt.cl", "TEACHER");
        when(teacherRepository.findByEmail("docente@ipt.cl")).thenReturn(Optional.of(teacher(1L)));

        assertDoesNotThrow(() -> accessControlService.requireSectionAccess(sectionOf(1L)));
    }

    @Test
    void requireSectionAccess_teacherOfAnotherSection_throwsForbidden() {
        login("docente@ipt.cl", "TEACHER");
        when(teacherRepository.findByEmail("docente@ipt.cl")).thenReturn(Optional.of(teacher(1L)));

        assertThrows(ForbiddenException.class,
                () -> accessControlService.requireSectionAccess(sectionOf(2L)));
    }

    // ---------- me ----------

    @Test
    void me_student_returnsRoleAndStudentId() {
        login("Ana@ipt.cl", "STUDENT");
        when(studentRepository.findByEmail("ana@ipt.cl")).thenReturn(Optional.of(student(5L)));
        when(teacherRepository.findByEmail("ana@ipt.cl")).thenReturn(Optional.empty());

        MeResponse me = accessControlService.me();

        assertEquals("ana@ipt.cl", me.email());
        assertEquals(List.of("STUDENT"), me.roles());
        assertEquals(5L, me.studentId());
        assertNull(me.teacherId());
    }

    @Test
    void me_teacher_returnsRoleAndTeacherId() {
        login("docente@ipt.cl", "TEACHER");
        when(teacherRepository.findByEmail("docente@ipt.cl")).thenReturn(Optional.of(teacher(3L)));
        when(studentRepository.findByEmail("docente@ipt.cl")).thenReturn(Optional.empty());

        MeResponse me = accessControlService.me();

        assertEquals(List.of("TEACHER"), me.roles());
        assertEquals(3L, me.teacherId());
        assertNull(me.studentId());
    }

    @Test
    void me_adminWithoutLinks_hasNoTeacherNorStudentId() {
        login("admin@ipt.cl", "ADMIN");

        MeResponse me = accessControlService.me();

        assertEquals(List.of("ADMIN"), me.roles());
        assertNull(me.teacherId());
        assertNull(me.studentId());
    }
}