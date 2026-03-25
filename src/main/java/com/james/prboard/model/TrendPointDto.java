package com.james.prboard.model;

import com.james.prboard.domain.constant.Discipline;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TrendPointDto {
    private String period;
    private Discipline discipline;
    private BigDecimal totalDistanceKm;
    private Long count;
}