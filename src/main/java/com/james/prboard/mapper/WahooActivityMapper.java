package com.james.prboard.mapper;

import com.james.prboard.domain.Activity;
import com.james.prboard.domain.ActivityEnduranceData;
import com.james.prboard.model.wahoo.WahooWorkoutResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WahooActivityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", expression = "java(String.valueOf(workout.getId()))")
    @Mapping(target = "source", constant = "WAHOO")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "startTime", source = "starts")
    @Mapping(target = "discipline", ignore = true)
    @Mapping(target = "canonicalActivity", ignore = true)
    @Mapping(target = "enduranceData", ignore = true)
    @Mapping(target = "weightliftingSession", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Activity toActivity(WahooWorkoutResponseDto workout);

    @Mapping(target = "activityId", ignore = true)
    @Mapping(target = "activity", ignore = true)
    @Mapping(target = "distanceMetres",
            expression = "java(workout.getWorkoutSummary() != null && workout.getWorkoutSummary().getDistanceAccum() != null ? java.math.BigDecimal.valueOf(workout.getWorkoutSummary().getDistanceAccum()) : null)")
    @Mapping(target = "durationSeconds", source = "workoutSummary.durationActiveAccum")
    @Mapping(target = "elevationGainMetres",
            expression = "java(workout.getWorkoutSummary() != null && workout.getWorkoutSummary().getAscentAccum() != null ? java.math.BigDecimal.valueOf(workout.getWorkoutSummary().getAscentAccum()) : null)")
    @Mapping(target = "avgHeartRate",
            expression = "java(workout.getWorkoutSummary() != null && workout.getWorkoutSummary().getHeartRateAverage() != null ? workout.getWorkoutSummary().getHeartRateAverage().shortValue() : null)")
    @Mapping(target = "maxHeartRate", ignore = true)
    @Mapping(target = "avgWatts",
            expression = "java(workout.getWorkoutSummary() != null && workout.getWorkoutSummary().getPowerBikeAvg() != null ? workout.getWorkoutSummary().getPowerBikeAvg().shortValue() : null)")
    @Mapping(target = "normalizedPower", ignore = true)
    @Mapping(target = "avgCadence",
            expression = "java(workout.getWorkoutSummary() != null && workout.getWorkoutSummary().getCadenceAvg() != null ? workout.getWorkoutSummary().getCadenceAvg().shortValue() : null)")
    ActivityEnduranceData toEnduranceData(WahooWorkoutResponseDto workout);
}