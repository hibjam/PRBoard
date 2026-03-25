package com.james.prboard.model;

import com.james.prboard.domain.constant.Discipline;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Set;

@Data
public class UserProfileDto {
    private String email;
    private Short heightCm;
    private BigDecimal weightKg;
    private String preferredUnits;
    private Set<Discipline> disciplines;
    private boolean profileComplete;
}