package com.james.prboard.model.wahoo;

import lombok.Data;

import java.util.List;

@Data
public class WahooWorkoutsPageDto {

    private List<WahooWorkoutResponseDto> workouts;

    private MetaDto meta;

    @Data
    public static class MetaDto {
        private Integer total;
    }
}