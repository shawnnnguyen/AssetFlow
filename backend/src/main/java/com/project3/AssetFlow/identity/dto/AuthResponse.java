package com.project3.AssetFlow.identity.dto;

public record AuthResponse(
        String token,
        String username,
        String role,
        Long userId
) {
}
