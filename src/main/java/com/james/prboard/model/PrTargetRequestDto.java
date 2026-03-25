package com.james.prboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PrTargetRequestDto {
    @JsonProperty("disciplineCode")
    private String disciplineCode;

    @JsonProperty("distanceMetres")
    private BigDecimal distanceMetres;

    @JsonProperty("label")
    private String label;
}
