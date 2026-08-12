package com.vaultdesk.backend.crypto;

/** Lancada quando a auth tag do GCM nao confere, indicando que o ciphertext foi adulterado. */
public class EncryptedPayloadTamperedException extends RuntimeException {

    public EncryptedPayloadTamperedException(Throwable cause) {
        super("Falha de integridade ao decriptografar conteudo", cause);
    }
}
