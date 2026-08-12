package com.vaultdesk.backend.auth;

import com.vaultdesk.backend.auth.dto.LoginRequest;
import com.vaultdesk.backend.auth.dto.RegisterRequest;
import com.vaultdesk.backend.common.exception.DuplicateEmailException;
import com.vaultdesk.backend.common.exception.InvalidRefreshTokenException;
import com.vaultdesk.backend.security.jwt.JwtProperties;
import com.vaultdesk.backend.security.jwt.JwtTokenProvider;
import com.vaultdesk.backend.user.User;
import com.vaultdesk.backend.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenTtl = Duration.ofDays(jwtProperties.refreshTokenTtlDays());
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()), request.displayName());
        return userRepository.save(user);
    }

    @Transactional
    public IssuedTokens login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado nao encontrado: " + request.email()));
        user.setLastLoginAt(Instant.now());

        return issueTokens(user);
    }

    @Transactional
    public IssuedTokens refresh(String rawRefreshToken) {
        RefreshToken existing = findActiveOrThrow(rawRefreshToken);
        existing.setRevoked(true);

        IssuedTokens tokens = issueTokens(existing.getUser());
        existing.setReplacedById(tokens.refreshTokenId());
        return tokens;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    private RefreshToken findActiveOrThrow(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token ausente");
        }
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token invalido"));
        if (!token.isActive()) {
            throw new InvalidRefreshTokenException("Refresh token expirado ou revogado");
        }
        return token;
    }

    private IssuedTokens issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        String rawRefreshToken = generateOpaqueToken();
        RefreshToken refreshToken =
                new RefreshToken(user, hash(rawRefreshToken), Instant.now().plus(refreshTokenTtl));
        refreshTokenRepository.save(refreshToken);

        return new IssuedTokens(
                accessToken, jwtTokenProvider.accessTokenTtlSeconds(), rawRefreshToken, refreshTokenTtl, refreshToken.getId());
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel", e);
        }
    }
}
