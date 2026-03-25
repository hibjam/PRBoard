package com.james.prboard.controller;

import com.james.prboard.model.DisciplineDto;
import com.james.prboard.repository.DisciplineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/disciplines")
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineRepository disciplineRepository;

    @GetMapping
    public List<DisciplineDto> getDisciplines() {
        return disciplineRepository.findByActiveTrue().stream()
                .map(d -> new DisciplineDto(d.getCode(), d.getDisplayName(), d.isDistanceBased()))
                .toList();
    }
}