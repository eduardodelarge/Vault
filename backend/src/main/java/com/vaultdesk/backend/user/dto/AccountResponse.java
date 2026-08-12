package com.vaultdesk.backend.user.dto;

import com.vaultdesk.backend.user.User;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id, String email, String displayName, Instant createdAt, Instant lastLoginAt) {

    public static AccountResponse from(User user) {
        return new AccountResponse(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt(), user.getLastLoginAt());
    }
}
