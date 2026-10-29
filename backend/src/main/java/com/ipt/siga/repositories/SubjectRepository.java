package com.ipt.siga.repositories;

import com.ipt.siga.entities.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByStudyPlanId(Long studyPlanId);

    List<Subject> findByStudyPlanIdAndSemester(Long studyPlanId, Integer semester);

    List<Subject> findByStudyPlanIdAndCodeIn(Long studyPlanId, Collection<String> codes);

    boolean existsByStudyPlanId(Long studyPlanId);

    boolean existsByStudyPlanIdAndCode(Long studyPlanId, String code);

    boolean existsByStudyPlanIdAndCodeAndIdNot(Long studyPlanId, String code, Long id);

    // Subjects that have the given subject as one of their prerequisites
    @Query("select s from Subject s join s.prerequisites p where p.id = :subjectId")
    List<Subject> findDependents(@Param("subjectId") Long subjectId);
}