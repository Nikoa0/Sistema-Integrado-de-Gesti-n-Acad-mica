package com.ipt.siga.repositories;

import com.ipt.siga.entities.StudyPlan;
import com.ipt.siga.enums.StudyPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    List<StudyPlan> findByCareerId(Long careerId);

    Optional<StudyPlan> findByCareerIdAndStatus(Long careerId, StudyPlanStatus status);

    boolean existsByCareerId(Long careerId);

    boolean existsByCareerIdAndIdentifier(Long careerId, String identifier);

    boolean existsByCareerIdAndIdentifierAndIdNot(Long careerId, String identifier, Long id);
}