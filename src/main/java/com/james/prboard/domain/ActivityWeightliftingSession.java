package com.james.prboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "activity_weightlifting_session")
@Getter
@Setter
@NoArgsConstructor
public class ActivityWeightliftingSession {

    @Id
    private Long activityId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "total_volume_kg", precision = 10, scale = 2)
    private BigDecimal totalVolumeKg;

    @Column(name = "total_sets")
    private Integer totalSets;

    @Column(name = "total_reps")
    private Integer totalReps;

    @Column(name = "bodyweight_kg", precision = 5, scale = 2)
    private BigDecimal bodyweightKg;

    @Column(name = "is_competition", nullable = false)
    private boolean competition = false;
}