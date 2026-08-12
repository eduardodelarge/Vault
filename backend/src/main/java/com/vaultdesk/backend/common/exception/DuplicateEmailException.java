package com.vaultdesk.backend.common.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Ja existe uma conta cadastrada com o email: " + email);
    }
}
