package com.james.prboard.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class UpdateProfileRequestDto {
    private Short heightCm;
    private BigDecimal weightKg;
    private String preferredUnits;
    private List<String> disciplineCodes;
}