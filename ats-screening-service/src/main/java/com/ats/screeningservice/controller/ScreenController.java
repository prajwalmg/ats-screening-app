package com.ats.screeningservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ats.screeningservice.dto.ScreenRequest;
import com.ats.screeningservice.dto.ScreenResponse;
import com.ats.screeningservice.service.ScreeningService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ScreenController {
    private final ScreeningService screeningService;

    @PostMapping("/screen")
    public ResponseEntity<ScreenResponse> screen(@Valid @RequestBody ScreenRequest request) {
        return ResponseEntity.ok(screeningService.screen(request));
    }
}
