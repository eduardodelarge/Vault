package com.vaultdesk.backend.auth;

import com.vaultdesk.backend.auth.dto.AuthResponse;
import com.vaultdesk.backend.auth.dto.LoginRequest;
import com.vaultdesk.backend.auth.dto.RegisterRequest;
import com.vaultdesk.backend.user.User;
import com.vaultdesk.backend.user.dto.AccountResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "vaultdesk_refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        IssuedTokens tokens = authService.login(request);
        return ResponseEntity.ok()
                .header("Set-Cookie", refreshCookie(tokens.refreshTokenRaw(), tokens.refreshTokenTtl().toSeconds()).toString())
                .body(new AuthResponse(tokens.accessToken(), tokens.accessTokenExpiresInSeconds()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        IssuedTokens tokens = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header("Set-Cookie", refreshCookie(tokens.refreshTokenRaw(), tokens.refreshTokenTtl().toSeconds()).toString())
                .body(new AuthResponse(tokens.accessToken(), tokens.accessTokenExpiresInSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header("Set-Cookie", refreshCookie("", 0).toString())
                .build();
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        // secure=false porque este ambiente roda localmente sobre HTTP; em producao com HTTPS deve ser true.
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
