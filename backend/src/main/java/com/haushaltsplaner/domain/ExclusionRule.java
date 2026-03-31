package com.haushaltsplaner.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exclusion_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExclusionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_a_id", nullable = false)
    private TaskTemplate taskA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_b_id", nullable = false)
    private TaskTemplate taskB;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type")
    @Builder.Default
    private ExclusionType ruleType = ExclusionType.MUTUAL;
}

enum ExclusionType {
    MUTUAL,      // Neither A nor B can have the same person
    ONE_WAY      // If A, then not B (but reverse allowed)
}
