package com.vaultdesk.backend.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vaultdesk.crypto")
public record CryptoProperties(String masterKeyBase64) {
}
