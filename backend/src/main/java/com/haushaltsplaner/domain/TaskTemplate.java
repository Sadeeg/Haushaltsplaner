package com.haushaltsplaner.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "task_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private TaskFrequency frequency;

    @Column(name = "default_points")
    @Builder.Default
    private Integer defaultPoints = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "completion_period_days")
    private Integer completionPeriodDays;
}
