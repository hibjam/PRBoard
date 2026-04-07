package com.james.prboard.mapper;

import com.james.prboard.domain.Activity;
import com.james.prboard.domain.ActivityEnduranceData;
import com.james.prboard.domain.ActivityWeightliftingSession;
import com.james.prboard.model.strava.StravaActivityResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StravaActivityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", expression = "java(String.valueOf(stravaActivity.getId()))")
    @Mapping(target = "source", constant = "STRAVA")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "startTime", source = "startDate")
    @Mapping(target = "discipline", ignore = true)
    @Mapping(target = "canonicalActivity", ignore = true)
    @Mapping(target = "enduranceData", ignore = true)
    @Mapping(target = "weightliftingSession", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Activity toActivity(StravaActivityResponseDto stravaActivity);

    @Mapping(target = "activityId", ignore = true)
    @Mapping(target = "activity", ignore = true)
    @Mapping(target = "distanceMetres",
            expression = "java(stravaActivity.getDistance() != null ? java.math.BigDecimal.valueOf(stravaActivity.getDistance()) : null)")
    @Mapping(target = "durationSeconds", source = "movingTime")
    @Mapping(target = "elevationGainMetres",
            expression = "java(stravaActivity.getTotalElevationGain() != null ? java.math.BigDecimal.valueOf(stravaActivity.getTotalElevationGain()) : null)")
    @Mapping(target = "avgHeartRate",
            expression = "java(stravaActivity.getAverageHeartrate() != null ? stravaActivity.getAverageHeartrate().shortValue() : null)")
    @Mapping(target = "maxHeartRate",
            expression = "java(stravaActivity.getMaxHeartrate() != null ? stravaActivity.getMaxHeartrate().shortValue() : null)")
    @Mapping(target = "avgWatts",
            expression = "java(stravaActivity.getAverageWatts() != null ? stravaActivity.getAverageWatts().shortValue() : null)")
    @Mapping(target = "normalizedPower",
            expression = "java(stravaActivity.getWeightedAverageWatts() != null ? stravaActivity.getWeightedAverageWatts().shortValue() : null)")
    @Mapping(target = "avgCadence",
            expression = "java(stravaActivity.getAverageCadence() != null ? stravaActivity.getAverageCadence().shortValue() : null)")
    ActivityEnduranceData toEnduranceData(StravaActivityResponseDto stravaActivity);

    @Mapping(target = "activityId", ignore = true)
    @Mapping(target = "activity", ignore = true)
    @Mapping(target = "durationSeconds", source = "movingTime")
    @Mapping(target = "totalVolumeKg", ignore = true)
    @Mapping(target = "totalSets", ignore = true)
    @Mapping(target = "totalReps", ignore = true)
    @Mapping(target = "bodyweightKg", ignore = true)
    @Mapping(target = "competition", ignore = true)
    ActivityWeightliftingSession toWeightliftingSession(StravaActivityResponseDto stravaActivity);
}