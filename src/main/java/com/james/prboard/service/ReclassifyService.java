package com.james.prboard.service;

import com.james.prboard.domain.Activity;
import com.james.prboard.domain.Discipline;
import com.james.prboard.domain.User;
import com.james.prboard.repository.ActivityRepository;
import com.james.prboard.repository.DisciplineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReclassifyService {

    private final ActivityRepository activityRepository;
    private final DisciplineRepository disciplineRepository;

    @Transactional
    public void reclassify(Long activityId, String newDisciplineCode, User currentUser) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + activityId));

        if (!activity.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new IllegalArgumentException("Activity not found: " + activityId);
        }

        Discipline newDiscipline = disciplineRepository.findByCode(newDisciplineCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown discipline: " + newDisciplineCode));

        Discipline current = activity.getDiscipline();

        if (current.getId().equals(newDiscipline.getId())) {
            return;
        }

        // Prevent reclassifying between endurance and weightlifting — the child
        // data tables are incompatible. Only allow within the same family.
        // Moving between lifting disciplines (WEIGHTLIFT ↔ POWERLIFTING ↔ OLYMPIC_LIFT)
        // is safe since they all share activity_weightlifting_session.
        if (current.isDistanceBased() != newDiscipline.isDistanceBased()) {
            throw new IllegalArgumentException(
                    "Cannot reclassify between endurance and weightlifting disciplines — " +
                            "the underlying data is incompatible");
        }

        log.info("Reclassifying activity {} from {} to {} for user {}",
                activityId, current.getCode(), newDisciplineCode, currentUser.getUserId());

        activity.setDiscipline(newDiscipline);
        activityRepository.save(activity);
    }
}