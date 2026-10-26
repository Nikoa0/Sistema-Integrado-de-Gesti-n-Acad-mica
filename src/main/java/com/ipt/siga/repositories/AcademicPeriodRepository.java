package com.ipt.siga.repositories;

import com.ipt.siga.entities.AcademicPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long> {

    boolean existsByAcademicYearAndSemester(Integer academicYear, Integer semester);

    List<AcademicPeriod> findAllByOrderByAcademicYearDescSemesterDesc();
}