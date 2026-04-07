package com.james.prboard.model.wahoo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class WahooWorkoutResponseDto {

    private Long id;

    private String name;

    private OffsetDateTime starts;

    @JsonProperty("workout_type")
    private WorkoutTypeDto workoutType;

    @JsonProperty("workout_summary")
    private WorkoutSummaryDto workoutSummary;

    @Data
    public static class WorkoutTypeDto {
        private String name;
    }

    @Data
    public static class WorkoutSummaryDto {

        @JsonProperty("distance_accum")
        private Double distanceAccum;

        @JsonProperty("duration_active_accum")
        private Integer durationActiveAccum;

        @JsonProperty("ascent_accum")
        private Double ascentAccum;

        @JsonProperty("heart_rate_average")
        private Double heartRateAverage;

        @JsonProperty("power_bike_avg")
        private Double powerBikeAvg;

        @JsonProperty("cadence_avg")
        private Double cadenceAvg;
    }
}