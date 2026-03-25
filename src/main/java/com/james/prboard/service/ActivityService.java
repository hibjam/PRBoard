package com.james.prboard.service;

import com.james.prboard.mapper.ActivityMapper;
import com.james.prboard.model.ActivityDto;
import com.james.prboard.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    @Transactional(readOnly = true)
    public List<ActivityDto> getActivitiesForUser(UUID userId) {
        return activityMapper.toActivityDtoList(
                activityRepository.findByUserUserIdAndCanonicalActivityIsNull(userId));
    }
}