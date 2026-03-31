package com.haushaltsplaner.repository;

import com.haushaltsplaner.domain.ExclusionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExclusionRuleRepository extends JpaRepository<ExclusionRule, Long> {
    List<ExclusionRule> findByHouseholdId(Long householdId);
}
