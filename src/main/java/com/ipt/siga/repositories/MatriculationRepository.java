package com.ipt.siga.repositories;

import com.ipt.siga.entities.Matriculation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatriculationRepository extends JpaRepository<Matriculation, Long> {

    boolean existsByStudentIdAndPeriodId(Long studentId, Long periodId);

    boolean existsByPeriodId(Long periodId);

    List<Matriculation> findByPeriodId(Long periodId);

    void deleteByStudentId(Long studentId);
}