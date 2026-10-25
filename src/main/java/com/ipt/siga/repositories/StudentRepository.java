package com.ipt.siga.repositories;

import com.ipt.siga.entities.Student;
import com.ipt.siga.enums.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRun(String run);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByCareerId(Long careerId);

    boolean existsByStudyPlanId(Long studyPlanId);

    List<Student> findByCareerId(Long careerId);

    List<Student> findByStatus(StudentStatus status);

    List<Student> findByCareerIdAndStatus(Long careerId, StudentStatus status);
}