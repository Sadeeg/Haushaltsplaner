package com.haushaltsplaner.repository;

import com.haushaltsplaner.domain.Task;
import com.haushaltsplaner.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    List<Task> findByAssignedUserIdAndStatus(Long userId, TaskStatus status);
    
    List<Task> findByDueDateBetweenAndStatus(LocalDate start, LocalDate end, TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.assignedUser.id = :userId AND t.dueDate = :date AND t.status = :status")
    List<Task> findByUserAndDateAndStatus(@Param("userId") Long userId, @Param("date") LocalDate date, @Param("status") TaskStatus status);
    
    @Query("SELECT t FROM Task t WHERE t.household.id = :householdId AND t.dueDate = :date")
    List<Task> findByHouseholdAndDate(@Param("householdId") Long householdId, @Param("date") LocalDate date);
    
    List<Task> findByHouseholdIdAndStatus(Long householdId, TaskStatus status);
}
