package com.james.prboard.controller;

import com.james.prboard.domain.User;
import com.james.prboard.model.PersonalRecordDto;
import com.james.prboard.model.PrTargetRequestDto;
import com.james.prboard.service.PersonalRecordQueryService;
import com.james.prboard.service.PersonalRecordService;
import com.james.prboard.service.UserResolutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stats/prs")
@Slf4j
@RequiredArgsConstructor
public class PersonalRecordController {

    private final PersonalRecordQueryService personalRecordQueryService;
    private final PersonalRecordService personalRecordService;
    private final UserResolutionService userResolutionService;

    @GetMapping
    public List<PersonalRecordDto> getPrs() {
        User user = userResolutionService.resolveCurrentUser();
        return personalRecordQueryService.getPrsForUser(user.getUserId());
    }

    @PostMapping("/recalculate")
    public ResponseEntity<Void> recalculate() {
        User user = userResolutionService.resolveCurrentUser();
        personalRecordService.recalculateAllPrs(user.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/targets")
    public ResponseEntity<Void> addTarget(@RequestBody PrTargetRequestDto request) {
        User user = userResolutionService.resolveCurrentUser();
        personalRecordQueryService.addCustomTarget(user.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/targets/{targetId}")
    public ResponseEntity<Void> deleteTarget(@PathVariable Integer targetId) {
        User user = userResolutionService.resolveCurrentUser();
        personalRecordQueryService.deleteTarget(user.getUserId(), targetId);
        return ResponseEntity.noContent().build();
    }
}