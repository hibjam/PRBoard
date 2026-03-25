package com.james.prboard.service;

import com.james.prboard.domain.Activity;
import com.james.prboard.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityDeduplicationService {

    private static final int WINDOW_MINUTES = 10;
    private static final double DURATION_TOLERANCE = 0.20;

    private final ActivityRepository activityRepository;

    /**
     * Called after each sync. Checks only the newly saved activities against
     * all existing canonicals for that user.
     */
    @Transactional
    public void deduplicateNewActivities(List<Activity> newActivities) {
        for (Activity activity : newActivities) {
            if (activity.getCanonicalActivity() != null) continue;
            findAndMarkDuplicate(activity);
        }
    }

    /**
     * Called once when a new source is connected for a user. Runs deduplication
     * across all activities for that user retroactively.
     */
    @Transactional
    public void deduplicateAll(UUID userId) {
        log.info("Running full deduplication for user {}", userId);

        List<Activity> all = activityRepository.findByUserUserIdAndCanonicalActivityIsNull(userId);
        int marked = 0;

        for (Activity activity : all) {
            if (activity.getCanonicalActivity() != null) continue;
            if (findAndMarkDuplicate(activity)) marked++;
        }

        log.info("Full deduplication complete for user {} — {} duplicates marked", userId, marked);
    }

    /**
     * Finds a canonical match for the given activity and marks it as a duplicate
     * if one is found. Returns true if the activity was marked as a duplicate.
     */
    private boolean findAndMarkDuplicate(Activity candidate) {
        OffsetDateTime from = candidate.getStartTime().minusMinutes(WINDOW_MINUTES);
        OffsetDateTime to   = candidate.getStartTime().plusMinutes(WINDOW_MINUTES);

        List<Activity> candidates = activityRepository.findDuplicateCandidates(
                candidate.getUser().getUserId(),
                candidate.getDiscipline().getId(),
                from,
                to
        );

        for (Activity existing : candidates) {
            if (existing.getId().equals(candidate.getId())) continue;
            if (existing.getCanonicalActivity() != null) continue;

            if (durationsMatch(existing, candidate)) {
                // Older activity (lower id) becomes canonical — first-wins.
                // When source priority is introduced, replace this with a
                // priority check: preferred source wins regardless of insert order.
                Activity duplicate = existing.getId() < candidate.getId() ? candidate : existing;
                Activity canonical = existing.getId() < candidate.getId() ? existing : candidate;

                duplicate.setCanonicalActivity(canonical);
                activityRepository.save(duplicate);

                log.info("Marked activity {} (source: {}) as duplicate of {} (source: {})",
                        duplicate.getId(), duplicate.getSource(),
                        canonical.getId(), canonical.getSource());
                return true;
            }
        }

        return false;
    }

    private boolean durationsMatch(Activity a, Activity b) {
        Integer durA = getDuration(a);
        Integer durB = getDuration(b);

        if (durA == null || durB == null) return false;
        if (durA == 0 || durB == 0) return false;

        double diff = Math.abs(durA - durB) / (double) Math.max(durA, durB);
        return diff <= DURATION_TOLERANCE;
    }

    private Integer getDuration(Activity activity) {
        if (activity.getEnduranceData() != null) {
            return activity.getEnduranceData().getDurationSeconds();
        }
        if (activity.getWeightliftingSession() != null) {
            return activity.getWeightliftingSession().getDurationSeconds();
        }
        return null;
    }
}