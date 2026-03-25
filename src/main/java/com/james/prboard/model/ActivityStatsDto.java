package com.james.prboard.model;

import com.james.prboard.domain.constant.Discipline;
import com.james.prboard.domain.constant.StatsPeriod;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ActivityStatsDto {
    private Discipline discipline;
    private String disciplineType;
    private StatsPeriod period;

    private Long count;
    private BigDecimal totalDistanceKm;
    private Long totalDurationSeconds;
    private BigDecimal longestActivityKm;
    private BigDecimal avgDistanceKm;
    private String avgPacePerKm;

    // Elevation — endurance only
    private BigDecimal totalElevationMetres;
    private BigDecimal bestElevationMetres;

    // Heart rate — endurance only
    private Short avgHeartRate;
    private Short peakAvgHeartRate;

    // Streak — always all-time regardless of period
    private Integer currentWeeklyStreak;
}