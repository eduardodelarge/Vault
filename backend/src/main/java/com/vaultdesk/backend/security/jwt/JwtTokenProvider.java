package com.vaultdesk.backend.security.jwt;

import com.vaultdesk.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.WeakKeyException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String EMAIL_CLAIM = "email";

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtTokenProvider(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException(
                    "vaultdesk.jwt.secret (VAULTDESK_JWT_SECRET) nao foi configurado");
        }
        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(properties.secret());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("VAULTDESK_JWT_SECRET deve ser uma string Base64 valida", e);
        }
        if (decoded.length < 32) {
            throw new WeakKeyException(
                    "VAULTDESK_JWT_SECRET deve decodificar para pelo menos 32 bytes (256 bits) para HS256");
        }
        this.key = new SecretKeySpec(decoded, "HmacSHA256");
        this.accessTokenTtl = Duration.ofMinutes(properties.accessTokenTtlMinutes());
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(EMAIL_CLAIM, user.getEmail())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .signWith(key)
                .compact();
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    /**
     * @throws JwtException se o token for invalido, malformado ou estiver expirado
     */
    public Claims validateAndParse(String token) {
        Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        return jws.getPayload();
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }
}
