package com.ipt.siga.repositories;

import com.ipt.siga.entities.Section;
import com.ipt.siga.enums.PeriodStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    boolean existsByPeriodId(Long periodId);

    boolean existsBySubjectId(Long subjectId);

    boolean existsByTeacherId(Long teacherId);

    boolean existsByTeacherIdAndPeriodStatus(Long teacherId, PeriodStatus status);

    boolean existsByPeriodIdAndSubjectId(Long periodId, Long subjectId);

    List<Section> findByPeriodId(Long periodId);

    List<Section> findByTeacherId(Long teacherId);

    List<Section> findByPeriodIdAndTeacherId(Long periodId, Long teacherId);

    // Sections of the offer that belong to a study plan
    List<Section> findByPeriodIdAndSubjectStudyPlanId(Long periodId, Long studyPlanId);

    // Locks the rows so that two students cannot take the last seat at the same time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Section> findByPeriodIdAndSubjectIdInOrderById(Long periodId, Collection<Long> subjectIds);
}