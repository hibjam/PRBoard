package com.james.prboard.domain.constant;

import java.time.OffsetDateTime;

public enum StatsPeriod {
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME;

    /**
     * Returns the start of this period relative to now.
     * Returns null for ALL_TIME — callers should treat null as no lower bound.
     */
    public OffsetDateTime startDate() {
        OffsetDateTime now = OffsetDateTime.now();
        return switch (this) {
            case WEEK  -> now.minusWeeks(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case MONTH -> now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case YEAR  -> now.minusYears(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case ALL_TIME -> null;
        };
    }
}