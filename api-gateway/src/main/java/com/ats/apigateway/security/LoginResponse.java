package com.ats.apigateway.security;

public record LoginResponse(String token, long expiresInMinutes) {
}
