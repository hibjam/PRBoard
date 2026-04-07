package com.james.prboard.model;

import com.james.prboard.domain.constant.Discipline;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PersonalRecordDto {
    private Discipline discipline;
    private String prType;
    private Integer targetId;        // null for non-distance PRs
    private String targetLabel;
    private BigDecimal targetDistanceMetres;
    private boolean preset;          // false = user-defined, can be deleted
    private BigDecimal value;
    private String formattedValue;
    private String activityName;
    private OffsetDateTime achievedAt;

    private String source;  // the source of the activity that set the PR
}