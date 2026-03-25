package com.james.prboard.model;

import com.james.prboard.domain.constant.Discipline;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class RecentActivityDto {
    private String externalId;
    private String name;
    private Discipline discipline;
    private BigDecimal distanceKm;
    private Integer durationSeconds;
    private OffsetDateTime startTime;
    private boolean indoor;
    private String pacePerKm;
}