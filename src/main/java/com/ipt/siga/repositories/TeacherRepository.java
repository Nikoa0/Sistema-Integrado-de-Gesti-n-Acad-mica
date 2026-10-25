package com.ipt.siga.repositories;

import com.ipt.siga.entities.Teacher;
import com.ipt.siga.enums.TeacherStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    boolean existsByRun(String run);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Teacher> findByStatus(TeacherStatus status);
}