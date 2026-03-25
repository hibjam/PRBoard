package com.james.prboard.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DisciplineDto {
    private String code;
    private String displayName;
    private boolean distanceBased;
}