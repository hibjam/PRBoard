package com.james.prboard.domain.constant;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class PrTargetDefaults {

    private PrTargetDefaults() {}

    public record PresetDistance(BigDecimal metres, String label) {}

    public static final Map<String, List<PresetDistance>> PRESETS = Map.of(
            "RUN", List.of(
                    new PresetDistance(new BigDecimal("1609.34"),  "1 mile"),
                    new PresetDistance(new BigDecimal("5000"),     "5K"),
                    new PresetDistance(new BigDecimal("10000"),    "10K"),
                    new PresetDistance(new BigDecimal("21097.5"),  "Half marathon"),
                    new PresetDistance(new BigDecimal("42195"),    "Marathon"),
                    new PresetDistance(new BigDecimal("50000"),    "50K"),
                    new PresetDistance(new BigDecimal("75000"),    "75K"),
                    new PresetDistance(new BigDecimal("100000"),   "100K")
            ),
            "TRAIL_RUN", List.of(
                    new PresetDistance(new BigDecimal("1609.34"),  "1 mile"),
                    new PresetDistance(new BigDecimal("5000"),     "5K"),
                    new PresetDistance(new BigDecimal("10000"),    "10K"),
                    new PresetDistance(new BigDecimal("21097.5"),  "Half marathon"),
                    new PresetDistance(new BigDecimal("42195"),    "Marathon"),
                    new PresetDistance(new BigDecimal("50000"),    "50K"),
                    new PresetDistance(new BigDecimal("75000"),    "75K"),
                    new PresetDistance(new BigDecimal("100000"),   "100K")
            ),
            "SWIM", List.of(
                    new PresetDistance(new BigDecimal("50"),       "50m"),
                    new PresetDistance(new BigDecimal("100"),      "100m"),
                    new PresetDistance(new BigDecimal("200"),      "200m"),
                    new PresetDistance(new BigDecimal("400"),      "400m"),
                    new PresetDistance(new BigDecimal("800"),      "800m"),
                    new PresetDistance(new BigDecimal("1500"),     "1500m"),
                    new PresetDistance(new BigDecimal("1609.34"),  "1 mile")
            ),
            "BIKE", List.of(
                    new PresetDistance(new BigDecimal("10000"),    "10K"),
                    new PresetDistance(new BigDecimal("25000"),    "25K"),
                    new PresetDistance(new BigDecimal("50000"),    "50K"),
                    new PresetDistance(new BigDecimal("100000"),   "100K"),
                    new PresetDistance(new BigDecimal("160934"),   "100 miles")
            ),
            "ROW", List.of(
                    new PresetDistance(new BigDecimal("500"),      "500m"),
                    new PresetDistance(new BigDecimal("1000"),     "1K"),
                    new PresetDistance(new BigDecimal("2000"),     "2K"),
                    new PresetDistance(new BigDecimal("5000"),     "5K"),
                    new PresetDistance(new BigDecimal("10000"),    "10K")
            ),
            "HIKE", List.of(
                    new PresetDistance(new BigDecimal("5000"),     "5K"),
                    new PresetDistance(new BigDecimal("10000"),    "10K"),
                    new PresetDistance(new BigDecimal("20000"),    "20K")
            )
    );

    public static List<PresetDistance> forDiscipline(String disciplineCode) {
        return PRESETS.getOrDefault(disciplineCode, List.of());
    }
}