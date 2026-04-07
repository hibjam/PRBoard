package com.james.prboard.service;

import com.james.prboard.domain.Activity;
import com.james.prboard.domain.ActivityEnduranceData;
import com.james.prboard.domain.ActivityWeightliftingSession;
import com.james.prboard.domain.constant.Discipline;
import com.james.prboard.domain.constant.StatsPeriod;
import com.james.prboard.model.ActivityStatsDto;
import com.james.prboard.model.RecentActivityDto;
import com.james.prboard.model.TrendPointDto;
import com.james.prboard.repository.ActivityRepository;
import com.james.prboard.repository.DisciplineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityStatsService {

    private final ActivityRepository activityRepository;
    private final DisciplineRepository disciplineRepository;

    @Transactional(readOnly = true)
    public List<ActivityStatsDto> getStatsPerDiscipline(UUID userId, StatsPeriod period) {
        List<com.james.prboard.domain.Discipline> userDisciplines =
                disciplineRepository.findByUserUserId(userId);

        if (userDisciplines.isEmpty()) return List.of();

        List<Activity> allActivities = activityRepository.findAllWithDataByUserUserId(userId);

        OffsetDateTime periodStart = period.startDate();
        List<Activity> periodActivities = periodStart == null ? allActivities :
                allActivities.stream()
                        .filter(a -> a.getStartTime().isAfter(periodStart))
                        .toList();

        Map<String, List<Activity>> byDisciplineCode = periodActivities.stream()
                .collect(Collectors.groupingBy(a -> a.getDiscipline().getCode()));

        Map<String, List<Activity>> allByDisciplineCode = allActivities.stream()
                .collect(Collectors.groupingBy(a -> a.getDiscipline().getCode()));

        return userDisciplines.stream()
                .map(disciplineEntity -> {
                    Discipline discipline = Discipline.fromCode(disciplineEntity.getCode());
                    List<Activity> activities = byDisciplineCode.getOrDefault(
                            disciplineEntity.getCode(), List.of());
                    List<Activity> allForStreak = allByDisciplineCode.getOrDefault(
                            disciplineEntity.getCode(), List.of());

                    if (disciplineEntity.isDistanceBased()) {
                        return buildEnduranceStats(discipline, activities, allForStreak, period);
                    } else {
                        return buildWeightliftingStats(discipline, activities, allForStreak, period);
                    }
                })
                .toList();
    }

    private ActivityStatsDto buildEnduranceStats(Discipline discipline,
                                                 List<Activity> activities,
                                                 List<Activity> allActivities,
                                                 StatsPeriod period) {
        List<ActivityEnduranceData> enduranceData = activities.stream()
                .map(Activity::getEnduranceData)
                .filter(e -> e != null)
                .toList();

        List<ActivityEnduranceData> withDistance = enduranceData.stream()
                .filter(e -> e.getDistanceMetres() != null
                        && e.getDistanceMetres().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        BigDecimal totalKm = toKm(withDistance.stream()
                .map(ActivityEnduranceData::getDistanceMetres)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        long totalSeconds = enduranceData.stream()
                .filter(e -> e.getDurationSeconds() != null)
                .mapToLong(ActivityEnduranceData::getDurationSeconds)
                .sum();

        BigDecimal longestKm = withDistance.stream()
                .map(ActivityEnduranceData::getDistanceMetres)
                .max(BigDecimal::compareTo)
                .map(this::toKm)
                .orElse(BigDecimal.ZERO);

        BigDecimal avgKm = withDistance.isEmpty() ? BigDecimal.ZERO :
                totalKm.divide(BigDecimal.valueOf(withDistance.size()), 2, RoundingMode.HALF_UP);

        String avgPace = (discipline == Discipline.RUN || discipline == Discipline.TRAIL_RUN)
                ? calculateAvgPace(withDistance) : null;

        BigDecimal totalElevation = enduranceData.stream()
                .filter(e -> e.getElevationGainMetres() != null)
                .map(ActivityEnduranceData::getElevationGainMetres)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal bestElevation = enduranceData.stream()
                .filter(e -> e.getElevationGainMetres() != null)
                .map(ActivityEnduranceData::getElevationGainMetres)
                .max(BigDecimal::compareTo)
                .orElse(null);

        double avgHr = enduranceData.stream()
                .filter(e -> e.getAvgHeartRate() != null && e.getAvgHeartRate() > 0)
                .mapToInt(ActivityEnduranceData::getAvgHeartRate)
                .average()
                .orElse(0);

        Short avgHeartRate = avgHr > 0 ? (short) Math.round(avgHr) : null;

        Short peakAvgHeartRate = enduranceData.stream()
                .filter(e -> e.getAvgHeartRate() != null && e.getAvgHeartRate() > 0)
                .mapToInt(ActivityEnduranceData::getAvgHeartRate)
                .max()
                .stream()
                .mapToObj(v -> (short) v)
                .findFirst()
                .orElse(null);

        int streak = calculateWeeklyStreak(allActivities);

        // Collect distinct sources from all activities in this period for this discipline
        Set<String> sources = activities.stream()
                .map(Activity::getSource)
                .filter(s -> s != null)
                .collect(Collectors.toSet());

        return new ActivityStatsDto(
                discipline, "ENDURANCE", period,
                (long) activities.size(), totalKm, (long) totalSeconds,
                longestKm, avgKm, avgPace,
                totalElevation.compareTo(BigDecimal.ZERO) > 0 ? totalElevation : null,
                bestElevation,
                avgHeartRate, peakAvgHeartRate,
                streak,
                sources
        );
    }

    private ActivityStatsDto buildWeightliftingStats(Discipline discipline,
                                                     List<Activity> activities,
                                                     List<Activity> allActivities,
                                                     StatsPeriod period) {
        List<ActivityWeightliftingSession> sessions = activities.stream()
                .map(Activity::getWeightliftingSession)
                .filter(s -> s != null)
                .toList();

        long totalSeconds = sessions.stream()
                .filter(s -> s.getDurationSeconds() != null)
                .mapToLong(ActivityWeightliftingSession::getDurationSeconds)
                .sum();

        BigDecimal totalVolume = sessions.stream()
                .filter(s -> s.getTotalVolumeKg() != null)
                .map(ActivityWeightliftingSession::getTotalVolumeKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal heaviestSession = sessions.stream()
                .filter(s -> s.getTotalVolumeKg() != null)
                .map(ActivityWeightliftingSession::getTotalVolumeKg)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal avgSessionVolume = sessions.isEmpty() ? BigDecimal.ZERO :
                totalVolume.divide(BigDecimal.valueOf(sessions.size()), 2, RoundingMode.HALF_UP);

        int streak = calculateWeeklyStreak(allActivities);

        // Collect distinct sources from all activities in this period for this discipline
        Set<String> sources = activities.stream()
                .map(Activity::getSource)
                .filter(s -> s != null)
                .collect(Collectors.toSet());

        return new ActivityStatsDto(
                discipline, "WEIGHTLIFTING", period,
                (long) activities.size(), totalVolume, (long) totalSeconds,
                heaviestSession, avgSessionVolume, null,
                null, null, null, null,
                streak,
                sources
        );
    }

    private int calculateWeeklyStreak(List<Activity> activities) {
        if (activities.isEmpty()) return 0;

        Set<String> activeWeeks = activities.stream()
                .map(a -> {
                    LocalDate date = a.getStartTime().toLocalDate();
                    int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                    int year = date.get(IsoFields.WEEK_BASED_YEAR);
                    return year + "-W" + String.format("%02d", week);
                })
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate current = LocalDate.now().with(DayOfWeek.MONDAY);

        while (true) {
            int week = current.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int year = current.get(IsoFields.WEEK_BASED_YEAR);
            String key = year + "-W" + String.format("%02d", week);
            if (!activeWeeks.contains(key)) break;
            streak++;
            current = current.minusWeeks(1);
        }

        return streak;
    }

    @Transactional(readOnly = true)
    public List<RecentActivityDto> getRecentActivities(UUID userId, int limit) {
        return activityRepository
                .findByUserUserIdAndCanonicalActivityIsNullOrderByStartTimeDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(activity -> {
                    RecentActivityDto dto = new RecentActivityDto();
                    dto.setExternalId(activity.getExternalId());
                    dto.setName(activity.getName());
                    dto.setDiscipline(Discipline.fromCode(activity.getDiscipline().getCode()));
                    dto.setStartTime(activity.getStartTime());
                    dto.setSource(activity.getSource());

                    ActivityEnduranceData endurance = activity.getEnduranceData();
                    if (endurance != null) {
                        dto.setDurationSeconds(endurance.getDurationSeconds());
                        boolean hasDistance = endurance.getDistanceMetres() != null
                                && endurance.getDistanceMetres().compareTo(BigDecimal.ZERO) > 0;
                        dto.setIndoor(!hasDistance);
                        dto.setDistanceKm(hasDistance ? toKm(endurance.getDistanceMetres()) : BigDecimal.ZERO);
                        Discipline discipline = Discipline.fromCode(activity.getDiscipline().getCode());
                        if (hasDistance && endurance.getDurationSeconds() != null
                                && (discipline == Discipline.RUN || discipline == Discipline.TRAIL_RUN)) {
                            dto.setPacePerKm(calculatePace(
                                    endurance.getDurationSeconds(), toKm(endurance.getDistanceMetres())));
                        }
                    } else {
                        ActivityWeightliftingSession session = activity.getWeightliftingSession();
                        if (session != null) dto.setDurationSeconds(session.getDurationSeconds());
                        dto.setIndoor(true);
                        dto.setDistanceKm(BigDecimal.ZERO);
                    }
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrendPointDto> getTrends(UUID userId, String groupBy, String from, String to) {
        List<Activity> activities = activityRepository
                .findByUserUserIdAndCanonicalActivityIsNull(userId);

        DateTimeFormatter formatter = groupBy.equals("weekly")
                ? DateTimeFormatter.ofPattern("yyyy-'W'ww")
                : DateTimeFormatter.ofPattern("yyyy-MM");

        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        List<Activity> filtered = activities.stream()
                .filter(a -> {
                    if (from != null && a.getStartTime().isBefore(OffsetDateTime.parse(from, isoFormatter))) return false;
                    if (to   != null && a.getStartTime().isAfter( OffsetDateTime.parse(to,   isoFormatter))) return false;
                    return true;
                })
                .toList();

        Map<String, Map<String, List<Activity>>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStartTime().format(formatter),
                        Collectors.groupingBy(a -> a.getDiscipline().getCode())
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(periodEntry -> periodEntry.getValue().entrySet().stream()
                        .map(disciplineEntry -> {
                            Discipline discipline = Discipline.fromCode(disciplineEntry.getKey());
                            List<Activity> periodActivities = disciplineEntry.getValue();
                            BigDecimal totalKm = periodActivities.stream()
                                    .map(Activity::getEnduranceData)
                                    .filter(e -> e != null && e.getDistanceMetres() != null)
                                    .map(ActivityEnduranceData::getDistanceMetres)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            return new TrendPointDto(periodEntry.getKey(), discipline,
                                    toKm(totalKm), (long) periodActivities.size());
                        }))
                .toList();
    }

    private BigDecimal toKm(BigDecimal metres) {
        if (metres == null) return BigDecimal.ZERO;
        return metres.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
    }

    private String calculatePace(int durationSeconds, BigDecimal distanceKm) {
        if (distanceKm.compareTo(BigDecimal.ZERO) == 0) return null;
        long paceSeconds = (long) (durationSeconds / distanceKm.doubleValue());
        return String.format("%d:%02d /km", paceSeconds / 60, paceSeconds % 60);
    }

    private String calculateAvgPace(List<ActivityEnduranceData> activities) {
        List<ActivityEnduranceData> valid = activities.stream()
                .filter(e -> e.getDurationSeconds() != null && e.getDistanceMetres() != null
                        && e.getDistanceMetres().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (valid.isEmpty()) return null;
        long totalSeconds = valid.stream().mapToLong(ActivityEnduranceData::getDurationSeconds).sum();
        BigDecimal totalKm = toKm(valid.stream().map(ActivityEnduranceData::getDistanceMetres)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return calculatePace((int) totalSeconds, totalKm);
    }
}