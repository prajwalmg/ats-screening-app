package com.ats.resumeparsingservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ats.resumeparsingservice.dto.ParseRequest;
import com.ats.resumeparsingservice.dto.ParsedResumeResponse;
import com.ats.resumeparsingservice.service.ResumeParsingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ParseController {
    private final ResumeParsingService resumeParsingService;

    @PostMapping("/parse")
    public ResponseEntity<ParsedResumeResponse> parse(@Valid @RequestBody ParseRequest request) {
        return ResponseEntity.ok(resumeParsingService.parse(request.resumeUrl()));
    }
}
