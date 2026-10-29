package com.ipt.siga.integration;

import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Career;
import com.ipt.siga.entities.Enrollment;
import com.ipt.siga.entities.Section;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.enums.PeriodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Process 1: academic enrollment (3 successful scenarios + 3 rejections)
@DisplayName("Process 1: Academic enrollment")
class EnrollmentFlowIT extends IntegrationTestBase {

    private Career career;
    private StudyPlan plan;
    private Teacher teacher;
    private AcademicPeriod period;
    private Student student;
    private Subject s1, s2, s3, s4;
    private Section sec1, sec2, sec3, sec4;

    @BeforeEach
    void setUp() {
        career = newCareer();
        plan = newPlan(career);
        s1 = newSubject(plan, "TAP101", 1);
        s2 = newSubject(plan, "TAP102", 1);
        s3 = newSubject(plan, "TAP103", 1);
        s4 = newSubject(plan, "TAP104", 1);

        teacher = newTeacher("docente@ipt.cl");
        period = newPeriod(2028, 1, PeriodStatus.OPEN);

        student = newStudent(career, plan, "ana@ipt.cl");
        matriculate(student, period);

        sec1 = newSection(period, s1, teacher, 30, "L1", "L2");
        sec2 = newSection(period, s2, teacher, 30, "M1", "M2");
        sec3 = newSection(period, s3, teacher, 30, "W1", "W2");
        sec4 = newSection(period, s4, teacher, 30, "J1", "J2");
    }

    private String body(Subject... subjects) {
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < subjects.length; i++) {
            ids.append(i == 0 ? "" : ",").append(subjects[i].getId());
        }
        return "{\"periodId\":" + period.getId() + ",\"subjectIds\":[" + ids + "]}";
    }

    private String enrollUrl(Student s) {
        return "/api/students/" + s.getId() + "/enrollments";
    }

    @Nested
    @DisplayName("Successful scenarios")
    class Successful {

        @Test
        @DisplayName("Enrolls three valid subjects: 201 and they are saved")
        void enroll_threeValidSubjects_returns201AndSavesThem() throws Exception {
            mockMvc.perform(post(enrollUrl(student))
                            .with(asStudent("ana@ipt.cl"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(s1, s2, s3)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(3));

            assertTrue(enrollmentRepository.existsByStudentIdAndPeriodId(student.getId(), period.getId()));
        }

        @Test
        @DisplayName("The offer returns the sections of the student's study plan")
        void offer_returnsTheSectionsOfTheStudentsStudyPlan() throws Exception {
            mockMvc.perform(get("/api/students/" + student.getId() + "/offer")
                            .param("periodId", period.getId().toString())
                            .with(asStudent("ana@ipt.cl")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(4));
        }

        @Test
        @DisplayName("Cancelling an enrollment in an open period: 204 and the seat is freed")
        void cancel_enrollmentInOpenPeriod_returns204AndFreesTheSeat() throws Exception {
            Enrollment enrollment = newEnrollment(student, s1, sec1, period, null);

            mockMvc.perform(delete("/api/enrollments/" + enrollment.getId())
                            .with(asStudent("ana@ipt.cl")))
                    .andExpect(status().isNoContent());

            assertFalse(enrollmentRepository.existsById(enrollment.getId()));
        }
    }

    @Nested
    @DisplayName("Rejection scenarios")
    class Rejections {

        @Test
        @DisplayName("Student not matriculated in the period: 409")
        void enroll_studentNotMatriculatedInThePeriod_returns409() throws Exception {
            Student notMatriculated = newStudent(career, plan, "luis@ipt.cl");

            mockMvc.perform(post(enrollUrl(notMatriculated))
                            .with(asStudent("luis@ipt.cl"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(s1, s2, s3)))
                    .andExpect(status().isConflict());

            assertFalse(enrollmentRepository.existsByStudentId(notMatriculated.getId()));
        }

        @Test
        @DisplayName("Subject with a non-approved prerequisite: 409")
        void enroll_subjectWithMissingPrerequisite_returns409() throws Exception {
            Subject s5 = newSubject(plan, "TAP201", 2);
            s5.getPrerequisites().add(s1); // s1 is not approved yet
            subjectRepository.save(s5);
            newSection(period, s5, teacher, 30, "V1", "V2");

            MvcResult result = mockMvc.perform(post(enrollUrl(student))
                            .with(asStudent("ana@ipt.cl"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(s5, s2, s3)))
                    .andExpect(status().isConflict())
                    .andReturn();

            assertTrue(result.getResponse().getContentAsString().contains("requires approved prerequisites"));
        }

        @Test
        @DisplayName("Section without available seats: 409")
        void enroll_sectionWithoutSeats_returns409() throws Exception {
            sec1.setCapacity(1);
            sectionRepository.save(sec1);
            Student other = newStudent(career, plan, "bea@ipt.cl");
            newEnrollment(other, s1, sec1, period, null); // takes the only seat

            MvcResult result = mockMvc.perform(post(enrollUrl(student))
                            .with(asStudent("ana@ipt.cl"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(s1, s2, s3)))
                    .andExpect(status().isConflict())
                    .andReturn();

            assertTrue(result.getResponse().getContentAsString().contains("no seats available"));
        }
    }
}