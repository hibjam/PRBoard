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
@Table(name = "activity_endurance_data")
@Getter
@Setter
@NoArgsConstructor
public class ActivityEnduranceData {

    @Id
    private Long activityId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(name = "distance_metres", precision = 10, scale = 2)
    private BigDecimal distanceMetres;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "elevation_gain_metres", precision = 8, scale = 2)
    private BigDecimal elevationGainMetres;

    @Column(name = "avg_heart_rate")
    private Short avgHeartRate;

    @Column(name = "max_heart_rate")
    private Short maxHeartRate;

    @Column(name = "avg_watts")
    private Short avgWatts;

    @Column(name = "normalized_power")
    private Short normalizedPower;

    @Column(name = "avg_cadence")
    private Short avgCadence;
}