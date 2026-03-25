package com.james.prboard.mapper;

import com.james.prboard.domain.Activity;
import com.james.prboard.domain.ActivityEnduranceData;
import com.james.prboard.domain.ActivityWeightliftingSession;
import com.james.prboard.model.ActivityDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ActivityMapper {

    @Mapping(target = "discipline",
            expression = "java(com.james.prboard.domain.constant.Discipline.fromCode(activity.getDiscipline().getCode()))")
    @Mapping(target = "disciplineType",
            expression = "java(activity.getDiscipline().isDistanceBased() ? \"ENDURANCE\" : \"WEIGHTLIFTING\")")
    @Mapping(target = "distanceKm",
            expression = "java(mapDistanceKm(activity))")
    @Mapping(target = "durationSeconds",
            expression = "java(mapDurationSeconds(activity))")
    ActivityDto toActivityDto(Activity activity);

    List<ActivityDto> toActivityDtoList(List<Activity> activities);

    default BigDecimal mapDistanceKm(Activity activity) {
        ActivityEnduranceData endurance = activity.getEnduranceData();
        if (endurance == null || endurance.getDistanceMetres() == null) return null;
        return endurance.getDistanceMetres()
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
    }

    default Integer mapDurationSeconds(Activity activity) {
        ActivityEnduranceData endurance = activity.getEnduranceData();
        if (endurance != null) return endurance.getDurationSeconds();
        ActivityWeightliftingSession session = activity.getWeightliftingSession();
        if (session != null) return session.getDurationSeconds();
        return null;
    }
}