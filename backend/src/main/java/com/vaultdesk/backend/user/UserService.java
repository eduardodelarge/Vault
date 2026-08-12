package com.vaultdesk.backend.user;

import com.vaultdesk.backend.auth.RefreshToken;
import com.vaultdesk.backend.auth.RefreshTokenRepository;
import com.vaultdesk.backend.common.exception.InvalidCurrentPasswordException;
import com.vaultdesk.backend.common.exception.ResourceNotFoundException;
import com.vaultdesk.backend.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        return userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado nao encontrado"));
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        for (RefreshToken token : refreshTokenRepository.findAllByUserIdAndRevokedFalse(user.getId())) {
            token.setRevoked(true);
        }
    }
}
