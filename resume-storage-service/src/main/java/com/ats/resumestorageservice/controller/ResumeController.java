package com.ats.resumestorageservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ats.resumestorageservice.dto.UploadResult;
import com.ats.resumestorageservice.service.ResumeStorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeStorageService resumeStorageService;

    @PostMapping(value = "/resumes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResult> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeStorageService.store(file));
    }
}
