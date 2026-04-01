package com.haushaltsplaner.repository;

import com.haushaltsplaner.domain.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, Long> {
    java.util.Optional<Household> findByInviteCode(String inviteCode);
}
