package com.ipt.siga.integration;

import com.ipt.siga.entities.AcademicPeriod;
import com.ipt.siga.entities.Career;
import com.ipt.siga.entities.Enrollment;
import com.ipt.siga.entities.Matriculation;
import com.ipt.siga.entities.Section;
import com.ipt.siga.entities.ScheduleBlock;
import com.ipt.siga.entities.Student;
import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.entities.Subject;
import com.ipt.siga.entities.Teacher;
import com.ipt.siga.enums.CareerStatus;
import com.ipt.siga.enums.PeriodStatus;
import com.ipt.siga.enums.StudentStatus;
import com.ipt.siga.enums.StudyPlanStatus;
import com.ipt.siga.enums.TeacherStatus;
import com.ipt.siga.repositories.AcademicPeriodRepository;
import com.ipt.siga.repositories.CareerRepository;
import com.ipt.siga.repositories.EnrollmentRepository;
import com.ipt.siga.repositories.MatriculationRepository;
import com.ipt.siga.repositories.SectionRepository;
import com.ipt.siga.repositories.StudentRepository;
import com.ipt.siga.repositories.StudyPlanRepository;
import com.ipt.siga.repositories.SubjectRepository;
import com.ipt.siga.repositories.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

// Starts the whole application against an in-memory H2 database. Every test runs in a
// transaction that is rolled back at the end, so each test starts from an empty database.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:siga_it;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/siga"
})
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired protected MockMvc mockMvc;

    @Autowired protected CareerRepository careerRepository;
    @Autowired protected StudyPlanRepository studyPlanRepository;
    @Autowired protected SubjectRepository subjectRepository;
    @Autowired protected TeacherRepository teacherRepository;
    @Autowired protected StudentRepository studentRepository;
    @Autowired protected AcademicPeriodRepository academicPeriodRepository;
    @Autowired protected MatriculationRepository matriculationRepository;
    @Autowired protected SectionRepository sectionRepository;
    @Autowired protected EnrollmentRepository enrollmentRepository;

    // Keycloak is not running in tests: the token is faked with jwt() in each request
    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ---------- fake authentication ----------

    protected RequestPostProcessor asAdmin() {
        return auth("admin@ipt.cl", "ADMIN");
    }

    protected RequestPostProcessor asTeacher(String email) {
        return auth(email, "TEACHER");
    }

    protected RequestPostProcessor asStudent(String email) {
        return auth(email, "STUDENT");
    }

    private RequestPostProcessor auth(String email, String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("email", email))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    // ---------- fixtures (saved straight through the repositories) ----------

    protected Career newCareer() {
        int n = SEQ.incrementAndGet();
        Career c = new Career();
        c.setCode("C" + n);
        c.setName("Career " + n);
        c.setDescription("Description");
        c.setStatus(CareerStatus.ACTIVE);
        c.setDurationSemesters(4);
        return careerRepository.save(c);
    }

    protected StudyPlan newPlan(Career career) {
        StudyPlan p = new StudyPlan();
        p.setCareer(career);
        p.setIdentifier("2021.3");
        p.setStatus(StudyPlanStatus.CURRENT);
        return studyPlanRepository.save(p);
    }

    // 2-0-2 weekly hours = 4 hours = 2 schedule blocks
    protected Subject newSubject(StudyPlan plan, String code, int semester) {
        Subject s = new Subject();
        s.setStudyPlan(plan);
        s.setCode(code);
        s.setName("Name " + code);
        s.setSemester(semester);
        s.setTheoryHours(2);
        s.setExerciseHours(0);
        s.setLabHours(2);
        s.setSct(5);
        return subjectRepository.save(s);
    }

    protected Teacher newTeacher(String email) {
        int n = SEQ.incrementAndGet();
        Teacher t = new Teacher();
        t.setRun(String.format("%08d-K", n));
        t.setFullName("Teacher " + n);
        t.setEmail(email);
        t.setProfessionalTitle("Engineer");
        t.setAcademicDegree("Master");
        t.setStatus(TeacherStatus.ACTIVE);
        return teacherRepository.save(t);
    }

    protected Student newStudent(Career career, StudyPlan plan, String email) {
        int n = SEQ.incrementAndGet();
        Student s = new Student();
        s.setRun(String.format("%08d-K", n));
        s.setFullName("Student " + n);
        s.setEmail(email);
        s.setCareer(career);
        s.setStudyPlan(plan);
        s.setStatus(StudentStatus.REGULAR);
        return studentRepository.save(s);
    }

    protected AcademicPeriod newPeriod(int year, int semester, PeriodStatus status) {
        AcademicPeriod p = new AcademicPeriod();
        p.setAcademicYear(year);
        p.setSemester(semester);
        p.setStatus(status);
        return academicPeriodRepository.save(p);
    }

    protected Matriculation matriculate(Student student, AcademicPeriod period) {
        Matriculation m = new Matriculation();
        m.setStudent(student);
        m.setPeriod(period);
        return matriculationRepository.save(m);
    }

    protected Section newSection(AcademicPeriod period, Subject subject, Teacher teacher,
                                 int capacity, String... blockCodes) {
        Set<ScheduleBlock> blocks = new HashSet<>();
        for (String code : blockCodes) {
            blocks.add(ScheduleBlock.fromCode(code));
        }
        Section s = new Section();
        s.setPeriod(period);
        s.setSubject(subject);
        s.setTeacher(teacher);
        s.setCapacity(capacity);
        s.setBlocks(blocks);
        return sectionRepository.save(s);
    }

    protected Enrollment newEnrollment(Student student, Subject subject, Section section,
                                       AcademicPeriod period, Double grade) {
        Enrollment e = new Enrollment();
        e.setStudent(student);
        e.setSubject(subject);
        e.setSection(section);
        e.setPeriod(period);
        e.setEnrolledAt(LocalDate.now());
        e.setGrade(grade);
        return enrollmentRepository.save(e);
    }
}