package com.james.prboard.service;

import com.james.prboard.domain.Activity;
import com.james.prboard.domain.ActivityEnduranceData;
import com.james.prboard.domain.ActivityWeightliftingSession;
import com.james.prboard.domain.PersonalRecord;
import com.james.prboard.domain.UserPrTarget;
import com.james.prboard.domain.constant.PrTargetDefaults;
import com.james.prboard.domain.constant.PrType;
import com.james.prboard.repository.ActivityRepository;
import com.james.prboard.repository.PersonalRecordRepository;
import com.james.prboard.repository.UserPrTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import com.james.prboard.repository.DisciplineRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonalRecordService {

    private final ActivityRepository activityRepository;
    private final PersonalRecordRepository personalRecordRepository;
    private final UserPrTargetRepository prTargetRepository;
    private final DisciplineRepository disciplineRepository;

    /**
     * Called after each sync or import.
     * Checks only the newly saved activities rather than rescanning everything.
     */
    @Transactional
    public void updatePrsForNewActivities(List<Activity> newActivities, UUID userId) {
        // Reload with child data within this transaction to avoid lazy init issues
        List<Long> ids = newActivities.stream().map(Activity::getId).toList();
        List<Activity> loaded = activityRepository.findAllWithDataByIds(ids);

        for (Activity activity : loaded) {
            if (activity.getCanonicalActivity() != null) continue;
            evaluateActivity(activity, userId);
        }
    }

    /**
     * Called when a new source is connected or on demand.
     * Full rescan of all activities — expensive but only run occasionally.
     */
    @Transactional
    public void recalculateAllPrs(UUID userId) {
        log.info("Recalculating all PRs for user {}", userId);

        // Clear existing PRs and rebuild from scratch
        List<PersonalRecord> existing = personalRecordRepository.findByUserId(userId);
        personalRecordRepository.deleteAll(existing);

        List<Activity> allActivities = activityRepository
                .findAllWithDataByUserUserId(userId);

        // Sort chronologically so first-occurrence wins on equal values
        allActivities.stream()
                .filter(a -> a.getCanonicalActivity() == null)
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .forEach(a -> evaluateActivity(a, userId));

        log.info("PR recalculation complete for user {}", userId);
    }

    private void evaluateActivity(Activity activity, UUID userId) {
        String disciplineCode = activity.getDiscipline().getCode();
        Short disciplineId = activity.getDiscipline().getId();

        ActivityEnduranceData endurance = activity.getEnduranceData();
        ActivityWeightliftingSession lifting = activity.getWeightliftingSession();

        if (endurance != null) {
            evaluateEndurance(activity, userId, disciplineId, disciplineCode, endurance);
        }
        if (lifting != null) {
            evaluateLifting(activity, userId, disciplineId, lifting);
        }
    }

    private void evaluateEndurance(Activity activity, UUID userId, Short disciplineId,
                                   String disciplineCode, ActivityEnduranceData endurance) {
        BigDecimal distance = endurance.getDistanceMetres();
        Integer duration = endurance.getDurationSeconds();

        if (distance == null || distance.compareTo(BigDecimal.ZERO) == 0) return;

        // Longest activity
        updatePrIfBetter(userId, disciplineId, PrType.LONGEST_ACTIVITY, null,
                distance, activity, false);

        // Best power (cycling only)
        if ("BIKE".equals(disciplineCode) && endurance.getAvgWatts() != null) {
            updatePrIfBetter(userId, disciplineId, PrType.BEST_POWER, null,
                    BigDecimal.valueOf(endurance.getAvgWatts()), activity, false);
        }

        // Fastest over target distances
        if (duration != null && duration > 0) {
            List<UserPrTarget> targets = prTargetRepository
                    .findByUserIdAndDisciplineId(userId, disciplineId);

            for (UserPrTarget target : targets) {
                if (isWithinTolerance(distance, target.getDistanceMetres())) {
                    // Normalise duration to exact target distance for fair comparison
                    BigDecimal normalisedSeconds = normaliseTime(duration, distance, target.getDistanceMetres());
                    // Lower is better for time
                    updatePrIfBetter(userId, disciplineId, PrType.FASTEST_DISTANCE,
                            target.getId(), normalisedSeconds, activity, true);
                }
            }
        }
    }

    private void evaluateLifting(Activity activity, UUID userId, Short disciplineId,
                                 ActivityWeightliftingSession lifting) {
        if (lifting.getTotalVolumeKg() != null) {
            updatePrIfBetter(userId, disciplineId, PrType.HEAVIEST_SESSION, null,
                    lifting.getTotalVolumeKg(), activity, false);
        }
    }

    /**
     * Updates or creates a PR if the new value beats the existing one.
     *
     * @param lowerIsBetter true for time-based PRs, false for distance/power/volume
     */
    private void updatePrIfBetter(UUID userId, Short disciplineId, String prType,
                                  Integer targetId, BigDecimal newValue,
                                  Activity activity, boolean lowerIsBetter) {
        PersonalRecord existing = targetId != null
                ? personalRecordRepository
                .findByUserIdAndDisciplineIdAndPrTypeAndTargetId(userId, disciplineId, prType, targetId)
                .orElse(null)
                : personalRecordRepository
                .findByUserIdAndDisciplineIdAndPrTypeAndTargetIsNull(userId, disciplineId, prType)
                .orElse(null);

        boolean isBetter = existing == null || (lowerIsBetter
                ? newValue.compareTo(existing.getValue()) < 0
                : newValue.compareTo(existing.getValue()) > 0);

        if (!isBetter) return;

        if (existing == null) {
            existing = new PersonalRecord();
            existing.setUserId(userId);
            existing.setDiscipline(activity.getDiscipline());
            existing.setPrType(prType);
            if (targetId != null) {
                prTargetRepository.findById(targetId).ifPresent(existing::setTarget);
            }
        }

        existing.setValue(newValue);
        existing.setActivity(activity);
        existing.setAchievedAt(activity.getStartTime());
        personalRecordRepository.save(existing);

        log.info("New PR — user: {}, discipline: {}, type: {}, value: {}",
                userId, activity.getDiscipline().getCode(), prType, newValue);
    }

    /**
     * A run counts toward a target if it is at least as long as the target.
     * The time is then normalised to the exact target distance using average pace.
     * e.g. a 30K run at 5:00/km pace gives a normalised 17K time of 1:25:00.
     */
    private boolean isWithinTolerance(BigDecimal actual, BigDecimal target) {
        return actual.compareTo(target) >= 0;
    }

    /**
     * Normalises a time to a target distance for fair comparison.
     * e.g. if someone ran 5050m in 25:00, their normalised 5K time is 24:45.
     */
    private BigDecimal normaliseTime(int durationSeconds, BigDecimal actualDistance,
                                     BigDecimal targetDistance) {
        double ratio = targetDistance.doubleValue() / actualDistance.doubleValue();
        return BigDecimal.valueOf(durationSeconds * ratio).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void seedDefaultTargets(UUID userId, String disciplineCode, Short disciplineId) {
        List<PrTargetDefaults.PresetDistance> presets = PrTargetDefaults.forDiscipline(disciplineCode);
        if (presets.isEmpty()) return;

        com.james.prboard.domain.Discipline discipline = disciplineRepository.findById(disciplineId)
                .orElseThrow(() -> new IllegalStateException("Discipline not found: " + disciplineId));

        for (PrTargetDefaults.PresetDistance preset : presets) {
            boolean exists = prTargetRepository
                    .findByUserIdAndDisciplineIdAndDistanceMetres(userId, disciplineId, preset.metres())
                    .isPresent();

            if (!exists) {
                UserPrTarget target = new UserPrTarget();
                target.setUserId(userId);
                target.setDiscipline(discipline);
                target.setDistanceMetres(preset.metres());
                target.setLabel(preset.label());
                target.setPreset(true);
                prTargetRepository.save(target);
            }
        }

        log.info("Seeded {} default PR targets for user {} discipline {}",
                presets.size(), userId, disciplineCode);
    }
}