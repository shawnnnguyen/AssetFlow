package com.project3.AssetFlow.identity.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        String username,
        String role,
        UUID userId
) {
}
