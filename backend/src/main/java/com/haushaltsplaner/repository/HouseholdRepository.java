package com.haushaltsplaner.repository;

import com.haushaltsplaner.domain.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, Long> {
    java.util.Optional<Household> findByInviteCode(String inviteCode);
    
    @Query("SELECT COUNT(m) FROM Household h JOIN h.members m WHERE h.id = :householdId")
    int countMembersByHouseholdId(@Param("householdId") Long householdId);
}
