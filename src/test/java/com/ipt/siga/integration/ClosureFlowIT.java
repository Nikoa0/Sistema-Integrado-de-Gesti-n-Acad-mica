package com.ipt.siga.integration;

import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Career;
import com.ipt.siga.entities.Section;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.enums.StudentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Process 3: academic period closure (3 successful scenarios + 3 rejections)
class ClosureFlowIT extends IntegrationTestBase {

    private Student student;
    private Subject s1, s2;            // the study plan has exactly these two subjects
    private AcademicPeriod period;     // 2028-1, OPEN: the one being closed
    private AcademicPeriod previous;   // 2027-2, CLOSED: academic history
    private Section section1;          // s1 in the open period
    private Section previousSection1;  // s1 in the previous period
    private Section previousSection2;  // s2 in the previous period

    @BeforeEach
    void setUp() {
        Career career = newCareer();
        StudyPlan plan = newPlan(career);
        s1 = newSubject(plan, "TAP101", 1);
        s2 = newSubject(plan, "TAP102", 1);
        Teacher teacher = newTeacher("docente@ipt.cl");

        previous = newPeriod(2027, 2, PeriodStatus.CLOSED);
        period = newPeriod(2028, 1, PeriodStatus.OPEN);

        student = newStudent(career, plan, "ana@ipt.cl");
        matriculate(student, period);

        previousSection1 = newSection(previous, s1, teacher, 30, "L1", "L2");
        previousSection2 = newSection(previous, s2, teacher, 30, "M1", "M2");
        section1 = newSection(period, s1, teacher, 30, "L1", "L2");
    }

    private String closeUrl(AcademicPeriod p) {
        return "/api/academic-periods/" + p.getId() + "/close";
    }

    // ---------- successful scenarios ----------

    @Test
    void close_allGradesRegistered_closesThePeriodAndKeepsThePendingStudentRegular() throws Exception {
        newEnrollment(student, s1, section1, period, 5.0); // s2 is still pending in the plan

        mockMvc.perform(post(closeUrl(period)).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.periodName").value("2028-1"))
                .andExpect(jsonPath("$.graduatedRuns.length()").value(0))
                .andExpect(jsonPath("$.eliminatedRuns.length()").value(0));

        assertEquals(PeriodStatus.CLOSED,
                academicPeriodRepository.findById(period.getId()).orElseThrow().getStatus());
        assertEquals(StudentStatus.REGULAR,
                studentRepository.findById(student.getId()).orElseThrow().getStatus());
    }

    @Test
    void close_studentFailsTheSameSubjectForTheSecondTime_isEliminated() throws Exception {
        newEnrollment(student, s1, previousSection1, previous, 3.0); // first failure, in history
        newEnrollment(student, s1, section1, period, 2.5);           // second failure, now

        mockMvc.perform(post(closeUrl(period)).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eliminatedRuns[0]").value(student.getRun()))
                .andExpect(jsonPath("$.graduatedRuns.length()").value(0));

        assertEquals(StudentStatus.ELIMINATED,
                studentRepository.findById(student.getId()).orElseThrow().getStatus());
    }

    @Test
    void close_studentApprovedEverySubjectOfThePlan_isGraduated() throws Exception {
        newEnrollment(student, s2, previousSection2, previous, 6.0); // s2 approved in history
        newEnrollment(student, s1, section1, period, 5.0);           // s1 approved now

        mockMvc.perform(post(closeUrl(period)).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graduatedRuns[0]").value(student.getRun()))
                .andExpect(jsonPath("$.eliminatedRuns.length()").value(0));

        assertEquals(StudentStatus.GRADUATED,
                studentRepository.findById(student.getId()).orElseThrow().getStatus());
    }

    // ---------- rejections ----------

    @Test
    void close_enrollmentsWithoutGrade_returns409AndKeepsThePeriodOpen() throws Exception {
        newEnrollment(student, s1, section1, period, null); // no grade registered

        MvcResult result = mockMvc.perform(post(closeUrl(period)).with(asAdmin()))
                .andExpect(status().isConflict())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("TAP101"));
        assertEquals(PeriodStatus.OPEN,
                academicPeriodRepository.findById(period.getId()).orElseThrow().getStatus());
    }

    @Test
    void close_periodAlreadyClosed_returns409() throws Exception {
        mockMvc.perform(post(closeUrl(previous)).with(asAdmin()))
                .andExpect(status().isConflict());
    }

    @Test
    void close_asTeacher_returns403AndKeepsThePeriodOpen() throws Exception {
        newEnrollment(student, s1, section1, period, 5.0);

        mockMvc.perform(post(closeUrl(period)).with(asTeacher("docente@ipt.cl")))
                .andExpect(status().isForbidden());

        assertEquals(PeriodStatus.OPEN,
                academicPeriodRepository.findById(period.getId()).orElseThrow().getStatus());
    }
}