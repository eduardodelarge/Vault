package com.vaultdesk.backend.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vaultdesk.jwt")
public record JwtProperties(String secret, int accessTokenTtlMinutes, int refreshTokenTtlDays) {
}
