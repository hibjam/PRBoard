package com.james.prboard.model.strava;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class StravaActivityResponseDto {

    private Long id;

    private String name;

    private String type;

    private Double distance;

    @JsonAlias("moving_time")
    private Integer movingTime;

    @JsonAlias("start_date")
    private OffsetDateTime startDate;

    @JsonAlias("total_elevation_gain")
    private Double totalElevationGain;

    @JsonAlias("average_heartrate")
    private Double averageHeartrate;

    @JsonAlias("max_heartrate")
    private Double maxHeartrate;

    @JsonAlias("average_watts")
    private Double averageWatts;

    @JsonAlias("weighted_average_watts")
    private Integer weightedAverageWatts;

    @JsonAlias("average_cadence")
    private Double averageCadence;
}