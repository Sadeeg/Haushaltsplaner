package com.haushaltsplaner.repository;

import com.haushaltsplaner.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByNextcloudId(String nextcloudId);
    Optional<User> findByTelegramChatId(Long telegramChatId);
    Optional<User> findByTelegramVerificationCode(String code);
}
