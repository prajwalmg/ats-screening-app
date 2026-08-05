package com.ats.resumestorageservice.dto;

public record UploadResult(String resumeUrl, String fileName, long sizeBytes) {
}
