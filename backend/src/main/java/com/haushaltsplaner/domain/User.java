package com.haushaltsplaner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    private String displayName;

    @Column(name = "telegram_chat_id")
    private Long telegramChatId;

    @Column(name = "telegram_verification_code")
    private String telegramVerificationCode;

    @Column(name = "nextcloud_id", unique = true)
    private String nextcloudId;

    @Column(name = "monthly_points")
    @Builder.Default
    private Integer monthlyPoints = 0;

    @Column(name = "last_points_reset")
    private LocalDateTime lastPointsReset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    private Household household;

    @OneToMany(mappedBy = "assignedUser", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Task> assignedTasks = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
