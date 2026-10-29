package com.ipt.siga.integration;

import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Enrollment;
import com.ipt.siga.entities.Section;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.entities.Career;
import com.ipt.siga.enums.PeriodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Process 2: registration of final grades (3 successful scenarios + 3 rejections)
@DisplayName("Process 2: Final grade registration")
class GradeFlowIT extends IntegrationTestBase {

    private Career career;
    private StudyPlan plan;
    private Subject subject;
    private Teacher teacher;
    private AcademicPeriod period;
    private Student student;
    private Section section;
    private Enrollment enrollment; // no grade yet

    @BeforeEach
    void setUp() {
        career = newCareer();
        plan = newPlan(career);
        subject = newSubject(plan, "TAP101", 1);
        teacher = newTeacher("docente@ipt.cl");
        period = newPeriod(2028, 1, PeriodStatus.OPEN);
        student = newStudent(career, plan, "ana@ipt.cl");
        matriculate(student, period);
        section = newSection(period, subject, teacher, 30, "L1", "L2");
        enrollment = newEnrollment(student, subject, section, period, null);
    }

    private String gradeUrl(Enrollment e) {
        return "/api/enrollments/" + e.getId() + "/grade";
    }

    private String body(double grade) {
        return "{\"grade\":" + grade + "}";
    }

    @Nested
    @DisplayName("Successful scenarios")
    class Successful {

        @Test
        @DisplayName("Responsible teacher registers the grade: 200 and result APPROVED")
        void register_responsibleTeacher_savesGradeAndReturnsApproved() throws Exception {
            mockMvc.perform(put(gradeUrl(enrollment))
                            .with(asTeacher("docente@ipt.cl"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(5.5)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.grade").value(5.5))
                    .andExpect(jsonPath("$.result").value("APPROVED"));

            assertEquals(5.5, enrollmentRepository.findById(enrollment.getId()).orElseThrow().getGrade());
        }

        @Test
        @DisplayName("Modifying an existing grade in an open period: 200 and result FAILED")
        void register_modifyingAnExistingGradeInOpenPeriod_returnsFailedResult() throws Exception {
            enrollment.setGrade(6.0);
            enrollmentRepository.save(enrollment);

            mockMvc.perform(put(gradeUrl(enrollment))
                            .with(asTeacher("docente@ipt.cl"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(3.5)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.grade").value(3.5))
                    .andExpect(jsonPath("$.result").value("FAILED"));
        }

        @Test
        @DisplayName("Student sees their own grade in their enrollments")
        void studentSeesHisOwnGradeInHisEnrollments() throws Exception {
            enrollment.setGrade(5.5);
            enrollmentRepository.save(enrollment);

            mockMvc.perform(get("/api/students/" + student.getId() + "/enrollments")
                            .with(asStudent("ana@ipt.cl")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].grade").value(5.5))
                    .andExpect(jsonPath("$[0].result").value("APPROVED"));
        }
    }

    @Nested
    @DisplayName("Rejection scenarios")
    class Rejections {

        @Test
        @DisplayName("Teacher of another section tries to register the grade: 403")
        void register_teacherOfAnotherSection_returns403() throws Exception {
            newTeacher("otro@ipt.cl");

            mockMvc.perform(put(gradeUrl(enrollment))
                            .with(asTeacher("otro@ipt.cl"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(5.0)))
                    .andExpect(status().isForbidden());

            assertNull(enrollmentRepository.findById(enrollment.getId()).orElseThrow().getGrade());
        }

        @Test
        @DisplayName("CLOSED period does not allow modifying grades: 409")
        void register_closedPeriod_returns409() throws Exception {
            AcademicPeriod closed = newPeriod(2027, 2, PeriodStatus.CLOSED);
            Section closedSection = newSection(closed, subject, teacher, 30, "M1", "M2");
            Enrollment closedEnrollment = newEnrollment(student, subject, closedSection, closed, 3.0);

            mockMvc.perform(put(gradeUrl(closedEnrollment))
                            .with(asTeacher("docente@ipt.cl"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(6.0)))
                    .andExpect(status().isConflict());

            assertEquals(3.0, enrollmentRepository.findById(closedEnrollment.getId()).orElseThrow().getGrade());
        }

        @Test
        @DisplayName("Grade outside the 1.0 - 7.0 range: 400")
        void register_gradeOutOfRange_returns400() throws Exception {
            mockMvc.perform(put(gradeUrl(enrollment))
                            .with(asTeacher("docente@ipt.cl"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(7.5)))
                    .andExpect(status().isBadRequest());

            assertNull(enrollmentRepository.findById(enrollment.getId()).orElseThrow().getGrade());
        }
    }
}