package com.james.prboard.domain.constant;

import java.util.Arrays;

public enum Discipline {
    SWIM,
    BIKE,
    RUN,
    TRAIL_RUN,
    ROW,
    HIKE,
    WEIGHTLIFT,
    POWERLIFTING,
    OLYMPIC_LIFT;

    public static Discipline fromCode(String code) {
        return Arrays.stream(values())
                .filter(d -> d.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown discipline code: " + code));
    }
}