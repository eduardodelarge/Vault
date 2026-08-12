package com.vaultdesk.backend.common.exception;

public class InvalidCurrentPasswordException extends RuntimeException {

    public InvalidCurrentPasswordException() {
        super("Senha atual incorreta");
    }
}
