package com.james.prboard.model;

import com.james.prboard.domain.constant.Discipline;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class ActivityDto {
    private String externalId;
    private String source;
    private Discipline discipline;
    private String disciplineType;   // "ENDURANCE" or "WEIGHTLIFTING"
    private BigDecimal distanceKm;
    private Integer durationSeconds;
    private OffsetDateTime startTime;
    private OffsetDateTime createdAt;
}