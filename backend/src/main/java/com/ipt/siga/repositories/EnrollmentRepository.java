package com.ipt.siga.repositories;

import com.ipt.siga.entities.Enrollment;
import com.ipt.siga.enums.PeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentId(Long studentId);

    boolean existsBySectionId(Long sectionId);

    boolean existsByStudentIdAndPeriodId(Long studentId, Long periodId);

    long countBySectionId(Long sectionId);

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByStudentIdAndPeriodId(Long studentId, Long periodId);

    List<Enrollment> findByPeriodId(Long periodId);

    List<Enrollment> findBySectionId(Long sectionId);

    // Subjects the student has approved: grade >= minimum in a CLOSED period
    @Query("select e.subject.id from Enrollment e "
            + "where e.student.id = :studentId and e.period.status = :status and e.grade >= :minGrade")
    List<Long> findApprovedSubjectIds(@Param("studentId") Long studentId,
                                      @Param("status") PeriodStatus status,
                                      @Param("minGrade") Double minGrade);

    // Enrollments of a period that still have no grade
    List<Enrollment> findByPeriodIdAndGradeIsNull(Long periodId);

    // How many times the student failed a subject in CLOSED periods
    @Query("select count(e) from Enrollment e "
            + "where e.student.id = :studentId and e.subject.id = :subjectId "
            + "and e.period.status = :status and e.grade < :minGrade")
    long countFailures(@Param("studentId") Long studentId,
                       @Param("subjectId") Long subjectId,
                       @Param("status") PeriodStatus status,
                       @Param("minGrade") Double minGrade);
}