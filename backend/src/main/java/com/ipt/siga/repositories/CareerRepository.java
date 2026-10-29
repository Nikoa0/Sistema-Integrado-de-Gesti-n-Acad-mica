package com.ipt.siga.repositories;

import com.ipt.siga.entities.Career;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CareerRepository extends JpaRepository<Career, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}