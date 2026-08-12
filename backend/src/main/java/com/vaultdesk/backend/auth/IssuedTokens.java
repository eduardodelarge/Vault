package com.vaultdesk.backend.auth;

import java.time.Duration;
import java.util.UUID;

record IssuedTokens(
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshTokenRaw,
        Duration refreshTokenTtl,
        UUID refreshTokenId) {
}
