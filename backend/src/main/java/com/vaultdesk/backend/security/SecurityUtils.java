package com.vaultdesk.backend.security;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UUID userId) {
            return userId;
        }
        throw new UsernameNotFoundException("Nenhum usuario autenticado no contexto de seguranca");
    }
}
