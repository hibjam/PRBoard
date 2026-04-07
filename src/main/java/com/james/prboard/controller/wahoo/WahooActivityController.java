package com.james.prboard.controller.wahoo;

import com.james.prboard.service.UserResolutionService;
import com.james.prboard.service.wahoo.WahooSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activities")
@Slf4j
@RequiredArgsConstructor
public class WahooActivityController {

    private final WahooSyncService wahooSyncService;
    private final UserResolutionService userResolutionService;

    @PostMapping("/sync/wahoo")
    public void syncWahooActivities() {
        var user = userResolutionService.resolveCurrentUser();
        wahooSyncService.syncActivities(user.getUserId(), user);
    }
}